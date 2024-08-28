#!/bin/python -u
#
# COPYRIGHT Ericsson 2022
#
#
#
# The copyright to the computer program(s) herein is the property of
#
# Ericsson Inc. The programs may be used and/or copied only with written
#
# permission from Ericsson Inc. or in accordance with the terms and
#
# conditions stipulated in the agreement/contract under which the
#
# program(s) have been supplied.
#

import sys
import argparse
import csv
import hashlib
import json
import logging
import os
import requests
import typing
import urllib3
from collections.abc import Sequence
from dataclasses import dataclass, field, is_dataclass, asdict
from itertools import count
from urllib3 import exceptions


id_generator = iter(count(start=0, step=1))
urllib3.disable_warnings(exceptions.InsecureRequestWarning)

parser = argparse.ArgumentParser(description='Super GeoSpatial Tool of Team Fremen')
parser.add_argument("-i", "--input", help="Path of the FlatFile",  default="/home/esuvben/eric-oss-rim-poc/geo_data.fdn.csv")
parser.add_argument("-n", "--hostname", default="so.599462422296.eu-west-1.ac.ericsson.se",
                    help="Hostname of Service Orchestrator")
parser.add_argument("-ds", "--data-sync-path", default="/cts/oss-core-ws/rest/osl-adv/datasync/process",
                    help="Path of the DataSync endpoint")
parser.add_argument("-u", "--username", default="so-user", help="Name of the user which can reach CTS")
parser.add_argument("-p", "--password", default="idunEr!css0n", help="Password of the user which can reach CTS")
parser.add_argument("-t", "--tenant", default="master", help="Tenant of the user which can reach CTS")
parser.add_argument("-c", "--chunk-size", default=500, type=int)
parser.add_argument("-H", "--header", nargs=2, action='append', help="Additional headers to CTS sync requests")
parser.add_argument('-l', "--logging", default="INFO", choices=['DEBUG', 'INFO'], help="Define logging level")
parser.add_argument('-o', "--output", help="Folder for storing payload data")
parser.add_argument('-mdk', "--md5key", default="EricssonENMAdapter-", help="Key for md5 encode")

def checkVersion():
    req_ver=(3,9)
    cur_ver = sys.version_info
    print("Python Version " + sys.version);
    if cur_ver < req_ver:
        print("---------------------------------------------------------------------------")
        print("THIS SCRIPT REQUIRES PYTHON 3.9 or Later")
        print("---------------------------------------------------------------------------")
        exit()

@dataclass
class Resource:
    TYPE = "ctc/resource"
    LINKS = ("action", "refId")

    refId: int = field(default_factory=id_generator.__next__, init=False)
    action: str = field(default_factory=lambda: "reconcile", init=False)
    status: str = field(default_factory=lambda: "operating", init=False)
    externalId: str = field(default=None, init=False)
    type: str = field(default=None, init=False)
    name: str


@dataclass
class GeospatialData:
    try:
        type: str = field(default="Point", init=False)
        coordinates: Sequence[float]    # [lon, lat, alt]
    except:
        print("An exception occurred in class: GeospatialData")
        checkVersion()


@dataclass
class GeographicLocation(Resource):
    TYPE = "ctg/geographicLocation"
    LINKS = ("geographicLocation",) + Resource.LINKS

    type: str = field(default_factory=lambda: "GeospatialCoords", init=False)
    geospatialData: GeospatialData


@dataclass
class AntennaModule(Resource):
    try:
        TYPE = "ctw/antennaModule"
        LINKS = ("geographicLocation",) + Resource.LINKS

        type: str = field(default_factory=lambda: "RF_MODULE", init=False)
        bearing: int
        geographicLocation: typing.Optional[list[int]] = None

        def set_location(self, location: GeographicLocation):
            self.geographicLocation = [location.refId]
    except:
        print("An exception occurred in class: AntennaModule")
        checkVersion()

# LogicalAntenna SectorEquipment NrSectorCarrier can be used later for fine-tuning the relations
@dataclass
class LogicalAntenna(Resource):
    try:
        TYPE = "ctw/logicalAntenna"
        LINKS = ("antennaModules",) + Resource.LINKS

        antennaModules: typing.Optional[list[int]] = None

        def set_module(self, module: AntennaModule):
            self.antennaModules = [module.refId]
    except:
        print("An exception occurred in class: LogicalAntenna")
        checkVersion()

@dataclass
class SectorEquipment(Resource):
    try:
        TYPE = "ctw/sectorEquipment"
        LINKS = ("supportedAntennas",) + Resource.LINKS
    
        supportedAntennas: typing.Optional[list[int]] = None
    
        def add_antenna(self, antenna: LogicalAntenna):
            if self.supportedAntennas is None:
                self.supportedAntennas = []
            self.supportedAntennas.append(antenna.refId)
    except:
        print("An exception occurred in class: SectorEquipment")
        checkVersion()

@dataclass
class NrSectorCarrier(Resource):
    try:
        TYPE = "ctw/nrSectorCarrier"
        LINKS = ("sectorEquipment",) + Resource.LINKS
    
        sectorEquipment: typing.Optional[list[int]] = None
    
        def add_equipment(self, equipment: SectorEquipment):
            if self.sectorEquipment is None:
                self.sectorEquipment = []
            self.sectorEquipment.append(equipment.refId)
    except:
        print("An exception occurred in class: NrSectorCarrier")
        checkVersion()

@dataclass
class GeographicSite(Resource):
    try:
        TYPE = "ctg/geographicSite"
        LINKS = ("locatedAt",) + Resource.LINKS
    
        type: str = field(default_factory=lambda: "Region", init=False)
        locatedAt: typing.Optional[list[int]] = None
    
        def add_location(self, location: GeographicLocation):
            if self.locatedAt is None:
                self.locatedAt = []
            self.locatedAt.append(location.refId)
    except:
        print("An exception occurred in class: GeographicSite")
        checkVersion()

@dataclass
class NrCell(Resource):
    try:
        TYPE = "ctw/nrCell"
        LINKS = ("nrSectorCarriers", "geographicSite") + Resource.LINKS
    
        nrSectorCarriers: list[int] = None
        geographicSite: list[int] = None
    
        def set_site(self, site: GeographicSite):
            self.geographicSite = [site.refId]
    
        def add_carrier(self, carrier: NrSectorCarrier):
            if self.nrSectorCarriers is None:
                self.nrSectorCarriers = []
            self.nrSectorCarriers.append(carrier.refId)
    except:
        print("An exception occurred in class: NrCell")
        checkVersion()


def create_payload(l_objects: Sequence[Resource]):
    payload = {
        "type": "osl-adv/datasyncservice/process",
        "jsonHolder": {
            "type": "gs/jsonHolder",
            "json": l_objects
        }
    }
    return payload


class EnhancedJSONEncoder(json.JSONEncoder):
    def default(self, o):
        if is_dataclass(o):
            if issubclass(type(o), Resource):
                return {"$type": o.TYPE} | dict(
                    map(lambda y: ('$' + y[0] if y[0] in o.LINKS else y[0], y[1]),
                        filter(lambda x: x[1] is not None, o.__dict__.items())))
            return asdict(o)
        return super().default(o)


def read_records(csv_path):
    return_value = {}
    with open(csv_path) as csvfile:
        reader = csv.DictReader(csvfile, delimiter=",", skipinitialspace=True)
        for record in reader:
            site = record.get("Site")
            cell = record.get("Cell")
            if record.get("fdn"):
                fdn = record["fdn"]
                _i = fdn.find("ManagedElement")
                site = fdn[fdn.index("=", _i) + 1:fdn.index(",", _i)]
                cell = site + "_" + fdn[fdn.rfind("=")+1:]
            if site not in return_value:
                return_value[site] = {"cells": {}, "coord": list(map(lambda x: record[x], ("lon", "lat")))}
            return_value[site]["cells"][cell] = dict(
                filter(lambda x: x[0] in ("ExternalId", "Azimuth", "bearing", "fdn"), record.items()))
    return return_value


def convert_record(site, site_data, md5key):
    geo_data = GeospatialData(site_data["coord"])
    location = GeographicLocation(site, geo_data)
    site = GeographicSite(site)
    site.add_location(location)
    return_value = [location, site]
    for cell, cell_data in site_data["cells"].items():
        external_id = cell_data.get("ExternalId", cell)
        if cell_data.get("fdn"):
            fdn = cell_data["fdn"]
            cell = "/".join(map(lambda x: x.split("=")[-1], cell_data["fdn"].split(",")))
            _i = fdn.index(",", fdn.find("ManagedElement"))
            external_id = f"{hashlib.md5((md5key+fdn[:_i]).encode()).hexdigest().upper()}{fdn[_i:].replace(',', '/ericsson-enm:')}"
        if cell_data["bearing"]:
            antenna = AntennaModule(cell, int(float(cell_data["bearing"]) * 10))
            antenna.set_location(location)
            return_value.append(antenna)
        nrcell = NrCell(cell)
        nrcell.externalId = external_id  # need to find the corresponding cellID
        nrcell.set_site(site)
        return_value.append(nrcell)
    return return_value


def main():
    session = requests.Session()
    args = parser.parse_args()
    logging.basicConfig(level=logging.getLevelName(args.logging))
    manual_run = bool(args.output)

    if manual_run:
        json_encoder = EnhancedJSONEncoder(indent=2)
    else:
        json_encoder = EnhancedJSONEncoder()

    login_url = f"https://{args.hostname}/auth/v1/login"
    data_sync_url = f"https://{args.hostname}{args.data_sync_path}"
    login_headers = {"X-Login": args.username, "X-password": args.password, "X-tenant": args.tenant}
    sync_headers = {"Content-Type": "application/json"} | {k: v for k, v in (args.header or [])}

    records = read_records(args.input)
    iterator = records.items().__iter__()

    if not manual_run:
        r = session.post(login_url, verify=False, headers=login_headers)
        r.raise_for_status()

    loop, i, current, size = True, 0, 0, len(records)
    logging.info(f"Progress {current}/{size} site")
    while loop:
        objects = []
        try:
            for i in range(1, args.chunk_size + 1):
                objects += convert_record(*iterator.__next__(), args.md5key)
        except StopIteration:
            loop, i = False, i-1
        payload_data = json_encoder.encode(create_payload(objects))
        if not manual_run:
            r = session.post(data_sync_url, verify=False, headers=sync_headers, data=payload_data)
            r.raise_for_status()
        else:
            with open(os.path.join(args.output, f"payload_{current}.json"), "w") as f:
                f.write(payload_data)
        current += i
        logging.info(f"Progress {current}/{size} site")

if __name__ == '__main__':
    main()

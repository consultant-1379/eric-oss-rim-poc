#!/bin/python -u
#
# COPYRIGHT Ericsson 2021
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

import argparse
import subprocess
import os
import boto3
from botocore.client import Config

parser = argparse.ArgumentParser(description='Object Store Tool of Team Fremen '
                                             'NOTE: mb.exe should be added to your PATH environment variable')

parser.add_argument("-n", "--hostname", default="http://localhost:9001",
                    help="Hostname and port of the Object Store", required=True)
parser.add_argument("-a", "--accesskey", default="rim_access_key",
                    help="Access key for the user", required=True)
parser.add_argument("-s", "--secretkey", default="rim_secret_key",
                    help="Secret key for the user", required=True)

subparser = parser.add_subparsers(dest='command')

create_user = subparser.add_parser('create_user')
create_bucket = subparser.add_parser('create_bucket')
push_data_to_bucket = subparser.add_parser('push_data')
set_expire_time_bucket = subparser.add_parser('set_expire_time_bucket')

create_user.add_argument("-aa", "--adminaccesskey", default="admin",
                         help="Access key for the admin user")
create_user.add_argument("-as", "--adminsecretkey", default="password",
                         help="Secret key for the new admin user")

create_bucket.add_argument("-b", "--bucketname", default="rim",
                           help="Name for the bucket to be created")
create_bucket.add_argument("-r", "--region", default="eu-west-2",
                           help="Region to use when creating the bucket")

push_data_to_bucket.add_argument("-b", "--bucketname", default="rim",
                                 help="Name of the bucket for data to be pushed to")
push_data_to_bucket.add_argument("-r", "--region", default="eu-west-2",
                                 help="Region to use when creating the bucket")
push_data_to_bucket.add_argument("-d", "--dir", help="Path for the directory to upload")

set_expire_time_bucket.add_argument("-b", "--bucketname", default="rim",
                           help="Name for the bucket which has been created")
set_expire_time_bucket.add_argument("-f", "--foldername", default="reports",
                           help="Folder name to set a expire day")
set_expire_time_bucket.add_argument("-dc", "--datecount", default="7",
                           help="Count of expire days")

def run_command(command):
    completed = subprocess.call(command, stdout=subprocess.PIPE, shell=True)
    print("\tCompleted '"  + command +  "' with exit code :" + str(completed))


def create_user(args):
    print('Creating user ' + args.accesskey)

    print('Configuring the host')
    command = f"mc config host add objstore {args.hostname} {args.adminaccesskey} {args.adminsecretkey}"
    run_command(command)

    print('Creating the new user')
    command = f"mc admin user add objstore {args.accesskey} {args.secretkey}"
    run_command(command)

    print('Adding readwrite policy to new user')
    command = f"mc admin policy set objstore  readwrite user={args.accesskey}"
    run_command(command)


def create_bucket(args):
    print('Creating bucket ' + args.bucketname)

    print('Configuring the host')
    command = f"mc config host add objstore {args.hostname} {args.accesskey} {args.secretkey}"
    run_command(command)

    print('Creating the new bucket')
    command = f"mc mb objstore/{args.bucketname} --region={args.region}"
    run_command(command)


def push_data(args):
    s3 = boto3.resource('s3',
                        endpoint_url=args.hostname,
                        aws_access_key_id=args.accesskey,
                        aws_secret_access_key=args.secretkey,
                        config=Config(signature_version='s3v4'),
                        region_name=args.region)

    source_dir = args.dir
    for subdir, dirs, files in os.walk(source_dir):
        for file in files:
            full_path = os.path.join(subdir, file)
            upload_path = full_path.replace(source_dir, '')
            s3.Bucket(args.bucketname).upload_file(full_path, upload_path)
            print(f"File {full_path} uploaded to {args.bucketname}/{upload_path}")

def set_expire_time_bucket(args):
    print('Setting the expire time for ' + args.bucketname + '/' + args.foldername)

    command = f"mc ilm add --prefix '{args.foldername}/' objstore/{args.bucketname}/ --expiry-days '{args.datecount}'"
    run_command(command)

def main():
    args = parser.parse_args()

    if args.command == 'create_user':
        create_user(args)
    elif args.command == 'create_bucket':
        create_bucket(args)
    elif args.command == 'push_data':
        push_data(args)
    elif args.command == 'set_expire_time_bucket':
        set_expire_time_bucket(args)
    else:
        print('Invalid option. Please see the help [-h] option. ')


if __name__ == '__main__':
    main()

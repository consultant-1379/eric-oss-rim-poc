--
-- COPYRIGHT Ericsson 2022
--
--
--
-- The copyright to the computer program(s) herein is the property of
--
-- Ericsson Inc. The programs may be used and/or copied only with written
--
-- permission from Ericsson Inc. or in accordance with the terms and
--
-- conditions stipulated in the agreement/contract under which the
--
-- program(s) have been supplied.
--

INSERT INTO GNODEB (DTYPE,ME_FDN,REF_ID,PARENT_REF_ID,GNBID,GNBID_LENGTH,MCC,MNC) VALUES
    ('GNBCUCPFunction','SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002','GNBCUCPFunction=1','',83,22,128,49),
    ('GNBCUCPFunction','SubNetwork=Ireland,MeContext=NR03gNodeBRadio00003,ManagedElement=NR03gNodeBRadio00003','GNBCUCPFunction=1','',84,22,128,49),
    ('ExternalGNBCUCPFunction','SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002','GNBCUCPFunction=1,NRNetwork=1,ExternalGNBCUCPFunction=NR03gNodeBRadio00003','GNBCUCPFunction=1,NRNetwork=1',84,22,128,49),
    ('ExternalGNBCUCPFunction','SubNetwork=Ireland,MeContext=NR03gNodeBRadio00003,ManagedElement=NR03gNodeBRadio00003','GNBCUCPFunction=1,NRNetwork=1,ExternalGNBCUCPFunction=NR03gNodeBRadio00001','GNBCUCPFunction=1,NRNetwork=1',83,22,128,49);

INSERT INTO NRCELLDU (ME_FDN,PARENT_REF_ID,REF_ID,ADMINISTRATIVE_STATE,NCI,P_ZERO_NOM_PUSCH_GRANT,P_ZERO_UE_PUSCH_OFFSET256QAM,SUB_CARRIER_SPACING,TDD_BORDER_VERSION,TDD_SPECIAL_SLOT_PATTERN,TDD_UL_DL_PATTERN, ADVANCED_DL_SU_MIMO_ENABLED) VALUES
	 ('SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002','GNBCUCPFunction=1','GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00002-1',1,1343813,-100,0,120,0,0,0,'false'),
	 ('SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002','GNBCUCPFunction=1','GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00002-2',1,1343593,-100,0,120,0,0,0,'false'),
	 ('SubNetwork=Ireland,MeContext=NR03gNodeBRadio00003,ManagedElement=NR03gNodeBRadio00003','GNBCUCPFunction=1','GNBDUFunction=1,NRCellCU=NR03gNodeBRadio00003-1',1,1360201,-100,0,120,0,0,0,'false');

INSERT INTO NRCELLCU (DTYPE,ME_FDN,PARENT_REF_ID,REF_ID,CELL_LOCAL_ID,NCI) VALUES
	 ('NRCellCU','SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002','GNBCUCPFunction=1','GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00002-1',325,1343813),
     ('NRCellCU','SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002','GNBCUCPFunction=1','GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00002-2',105,1343593),
     ('NRCellCU','SubNetwork=Ireland,MeContext=NR03gNodeBRadio00003,ManagedElement=NR03gNodeBRadio00003','GNBCUCPFunction=1','GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00003-1',329,1360201);

INSERT INTO NRCELLCU (DTYPE,ME_FDN,PARENT_REF_ID,REF_ID,CELL_LOCAL_ID) VALUES
    ('ExternalNRCellCU','SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002','GNBCUCPFunction=1,NRNetwork=1,ExternalGNBCUCPFunction=NR03gNodeBRadio00003','GNBCUCPFunction=1,NRNetwork=1,ExternalGNBCUCPFunction=NR03gNodeBRadio00003,ExternalNRCellCU=NR03gNodeBRadio00003-1',329),
    ('ExternalNRCellCU','SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002','GNBCUCPFunction=1,NRNetwork=1,ExternalGNBCUCPFunction=NR03gNodeBRadio00003','GNBCUCPFunction=1,NRNetwork=1,ExternalGNBCUCPFunction=NR03gNodeBRadio00003,ExternalNRCellCU=NR03gNodeBRadio00003-2',330),
    ('ExternalNRCellCU','SubNetwork=Ireland,MeContext=NR03gNodeBRadio00003,ManagedElement=NR03gNodeBRadio00003','GNBCUCPFunction=1,NRNetwork=1,ExternalGNBCUCPFunction=NR03gNodeBRadio00001','GNBCUCPFunction=1,NRNetwork=1,ExternalGNBCUCPFunction=NR03gNodeBRadio00001,ExternalNRCellCU=NR03gNodeBRadio00001-2',325);

INSERT INTO NRCELL_RELATION (ME_FDN,PARENT_REF_ID,REF_ID,CELL_INDIVIDUAL_OFFSETNR,NRCELL_ME_FDN,NRCELL_REF_ID) VALUES
	 ('SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002','GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00002-1','GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00002-1,NRCellRelation=105',0,'SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002','GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00002-2'),
	 ('SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002','GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00002-1','GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00002-1,NRCellRelation=auto105',0,'SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002','GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00002-2'),
	 ('SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002','GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00002-2','GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00002-2,NRCellRelation=325',0,'SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002','GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00002-1'),
	 ('SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002','GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00002-1','GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00002-1,NRCellRelation=297',0,'SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002','GNBCUCPFunction=1,NRNetwork=1,ExternalGNBCUCPFunction=NR03gNodeBRadio00003,ExternalNRCellCU=NR03gNodeBRadio00003-1'),
	 ('SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002','GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00002-1','GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00002-1,NRCellRelation=298',0,'SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002','GNBCUCPFunction=1,NRNetwork=1,ExternalGNBCUCPFunction=NR03gNodeBRadio00003,ExternalNRCellCU=NR03gNodeBRadio00003-2'),
	 ('SubNetwork=Ireland,MeContext=NR03gNodeBRadio00002,ManagedElement=NR03gNodeBRadio00002','GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00002-1','GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00002-1,NRCellRelation=324',0,'SubNetwork=Ireland,MeContext=NR03gNodeBRadio00003,ManagedElement=NR03gNodeBRadio00003','GNBCUCPFunction=1,NRNetwork=1,ExternalGNBCUCPFunction=NR03gNodeBRadio00001,ExternalNRCellCU=NR03gNodeBRadio00001-1'),
	 ('SubNetwork=Ireland,MeContext=NR03gNodeBRadio00003,ManagedElement=NR03gNodeBRadio00003','GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00003-1','GNBCUCPFunction=1,NRCellCU=NR03gNodeBRadio00003-1,NRCellRelation=325',5,'SubNetwork=Ireland,MeContext=NR03gNodeBRadio00003,ManagedElement=NR03gNodeBRadio00003','GNBCUCPFunction=1,NRNetwork=1,ExternalGNBCUCPFunction=NR03gNodeBRadio00001,ExternalNRCellCU=NR03gNodeBRadio00001-2');
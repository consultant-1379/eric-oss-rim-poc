--
-- COPYRIGHT Ericsson 2023
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


INSERT INTO PM_ROPNRCELLDU ( USE_PRE_CALCULATED_IN_CSV, AVG_DELTA_IPN, AVG_SYMBOL_DELTA_IPN, TOTAL_BIN_SUM_SYMBOL_DELTA_IPN, TOTAL_BIN_SUM_MAX_DELTA_IPN, PM_MAC_VOL_UL_RES_UE, PM_MAC_TIME_UL_RES_UE, POSITIVE_BIN_SUM_SYMBOL_DELTA_IPN, FDN , ROP_TIME ) VALUES
(false, 100  , 100  , 300  , 300  , 100 , 100  , 200 , 'fdn1', 0),
(false, 7    , 7    , 300  , 300  , 8   , 8    , 150 , 'fdn1', 900000),
(false, 6    , 6    , 100  , 100  , 7   , 7    , 50  , 'fdn1', 1800000),
(false, 5    , 5    , NULL , NULL , 6   , 6    , 200 , 'fdn1', 2700000),
(false, 'NaN', 'NaN', 300  , 300  , 5   , 5    , NULL, 'fdn1', 3600000),
(false, 4    , 4    , 300  , 300  , 4   , 4    , 100 , 'fdn1', 4500000),
(false, 3    , 3    , 300  , 300  , 3   , 3    , 100 , 'fdn1', 5400000),
(false, 2    , 2    , 300  , 300  , 2   , NULL , 100 , 'fdn1', 6300000),
(false, 1    , 1    , 300  , 300  , NULL, 1    , 100 , 'fdn1', 7200000),
(false, NULL , NULL , NULL , NULL , 0   , 0    , NULL, 'fdn2', 0 ),
(false, NULL , NULL , NULL , NULL , 8   , 8    , NULL, 'fdn2', 900000),
(false, NULL , NULL , NULL , NULL , 7   , 7    , NULL, 'fdn2', 1800000),
(false, NULL , NULL , NULL , NULL , 6   , 6    , NULL, 'fdn2', 2700000),
(false, NULL , NULL , NULL , NULL , 5   , 5    , NULL, 'fdn2', 3600000),
(false, NULL , NULL , NULL , NULL , 4   , 4    , NULL, 'fdn2', 4500000),
(false, NULL , NULL , NULL , NULL , 3   , 3    , NULL, 'fdn2', 5400000),
(false, NULL , NULL , NULL , NULL , 2   , 0    , NULL, 'fdn2', 6300000),
(false, NULL , NULL , NULL , NULL , NULL, 0    , NULL, 'fdn2', 7200000),
(false, NULL , NULL , NULL , NULL , 0   , 0    , NULL, 'fdn3', 0 ),
(false, NULL , NULL , NULL , NULL , 8   , 0    , NULL, 'fdn3', 900000),
(false, NULL , NULL , NULL , NULL , 7   , 0    , NULL, 'fdn3', 1800000),
(false, NULL , NULL , NULL , NULL , 6   , 0    , NULL, 'fdn3', 2700000),
(false, NULL , NULL , NULL , NULL , 5   , 0    , NULL, 'fdn3', 3600000),
(false, NULL , NULL , NULL , NULL , 4   , 0    , NULL, 'fdn3', 4500000),
(false, NULL , NULL , NULL , NULL , 3   , 0    , NULL, 'fdn3', 5400000),
(false, NULL , NULL , NULL , NULL , 2   , 0    , NULL, 'fdn3', 6300000),
(false, NULL , NULL , NULL , NULL , NULL, 0    , NULL, 'fdn3', 7200000),
(false, 100  , 100  , 300  , 300  , 100 , 100  , NULL, 'fdn4', 0),
(false, 7    , 7    , 300  , 300  , 16  , 8    , NULL, 'fdn4', 900000),
(false, 6    , 6    , 300  , 300  , 14  , 7    , NULL, 'fdn4', 1800000),
(false, 5    , 5    , 300  , 300  , 12  , 6    , NULL, 'fdn4', 2700000),
(false, 4    , 4    , 300  , 300  , 10  , 5    , NULL, 'fdn4', 3600000),
(false, 4    , 4    , 300  , 300  , 8   , 4    , NULL, 'fdn4', 4500000),
(false, 3    , 3    , 300  , 300  , 6   , 3    , NULL, 'fdn4', 5400000),
(false, 2    , 2    , 300  , 300  , 4   , 2    , NULL, 'fdn4', 6300000),
(false, 1    , 1    , 300  , 300  , 1   , 1    , NULL, 'fdn4', 7200000),
(false, 3    , 3    , 300  , 300  , 6   , 3    , NULL, 'fdn5', 5400000),
(false, 2    , 2    , 300  , 300  , 4   , 2    , NULL, 'fdn5', 6300000),
(false, 0    , 0    , 0    , 900  , 0   , 0    , 0   , 'fdn6', 4500000),
(false, 0    , 0    , 0    , 900  , 0   , 0    , 0   , 'fdn6', 5400000),
(false, 0    , 0    , 0    , 900  , 0   , 0    , 0   , 'fdn6', 6300000),
(false, 0    , 0    , 0    , 900  , 0   , 0    , 0   , 'fdn6', 7200000);
# XML Schema Documents for CTC Phase 5

The XSDs and Zip files in this directory are schema definitions for messages sent to and from the CTC Traders system. They are intended to allow developers to write schema valid messages, however please be aware that there are also business rules that will be applied, and that messages that conform to these schema are not guaranteed to be accepted by the system. 

The following resources will be helpful in understanding how to use these XSDs further:

* [NCTS Phase 5 Technical Interface Specification](https://developer.service.hmrc.gov.uk/guides/ctc-traders-phase5-tis/)
* [CTC Traders API phase 5 service guide](https://developer.service.hmrc.gov.uk/guides/ctc-traders-phase5-service-guide)

The XSDs are also contained in the ALL_CTC_XSDs_v51.8.2_B.zip file.

## XSD versions

The XSDs in this directory are version 51.8.2. The version currently deployed on Trader Test is version 51.8_A, which are available from the ALL_CTC_XSDs_v51.8_A.zip file.

**Note that this version retains the leading space for phase ID " NCTS5.1" in stypes.** Please feel free to change this locally -- this leading space has no effect on the validation. 
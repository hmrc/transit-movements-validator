#!/usr/bin/python3
"""
Copyright 2022 HM Revenue & Customs

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""
import json
import os
import re
import glob

"""
This script is intended for internal use, where a set of schema to be processed
fresh from XMLSpy are placed in the directory "old", with an empty directory "new", 
and running this script will perform the processing required. 

Note that the filename for each message type should contain "ccXXXc", where XXX is the number
of the message.
"""

def hook(fname):

    code = re.search('cc([0-9]{3})c', fname)
    if code is not None and code.group(1) is not None:
        return createHook(code="CC" + code.group(1) + "C")
    else:
        return createHook()


def createHook(code=None):

    def properties_object_hook(dct):
        """
        Takes a dictionary and determines if it's a candidate for transformation
        i.e. if it has a properties entry and, if so, if it contains # and $
        fields, and if $ points to a $ref. If so, we transform it so that the ref
        is on the main body of this object.

        We do this so that we remove the requirement for adding $ fields, so the
        structure of the Json will be very similar to the XML, only in a different
        format.

        :param dct: The dictionary to inspect
        :return: The modified dictionary
        """
        if code is not None and 'n1:MessageTypes' in dct:
            properties = dct['n1:MessageTypes']
            properties['enum'] = [code]

        if 'properties' in dct:
            properties = dct['properties']
            if '$' in properties and '#' in properties:
                dct['$ref'] = properties['$']['$ref']
                del dct['additionalProperties']
                del dct['properties']
                del dct['type']
                del dct['patternProperties']

        if 'description' in dct and isinstance(dct['description'], str):
            desc = dct['description'].strip()
            if desc:  # is truthy if not an empty string
                dct['description'] = desc
            else:
                del dct['description']

        return dct

    return properties_object_hook


# end def

filepath = os.path.join(os.getcwd(), 'old')
newFilePath = os.path.join(os.getcwd(), 'new')

os.chdir(filepath)

files = glob.glob(os.path.join(filepath, "*.json"), recursive=False)

for file in files:
    filename = file.rsplit("/", 1)[1]

    object_hook = hook(filename)

    oldName = file
    newName = os.path.join(newFilePath, filename)
    print("Transforming file {} and saving to {}".format(oldName, newName))

    with open(file) as f:
        old = json.load(f, object_hook=object_hook)

        with open(newName, mode='w') as n:
            n.write(json.dumps(old, indent=2))


# endfor

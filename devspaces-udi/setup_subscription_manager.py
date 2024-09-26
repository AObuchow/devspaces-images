import os # TODO: Cleanup these imports so we only import functions we need 
from os import listdir
import sys
import shutil

KEYS_DIR="subscription-manager"
RHSM_CA_DIR="rhsm-ca"
RHSM_CONF_DIR="rhsm-conf"
PKI_ENTITLEMENT_DIR="etc-pki-entitlement"

keys_parent_dir = os.path.join(os.getcwd(), KEYS_DIR)

rhsm_ca_dir = os.path.join(keys_parent_dir, RHSM_CA_DIR)
rhms_conf_dir = os.path.join(keys_parent_dir, RHSM_CONF_DIR)
pki_entitlement_dir = os.path.join(keys_parent_dir, PKI_ENTITLEMENT_DIR)

# Create directories
if not os.path.isdir(keys_parent_dir):
    os.mkdir(keys_parent_dir)

if not os.path.isdir(rhsm_ca_dir):
    os.mkdir(rhsm_ca_dir)

if not os.path.isdir(rhms_conf_dir):
    os.mkdir(rhms_conf_dir)

if not os.path.isdir(pki_entitlement_dir):
    os.mkdir(pki_entitlement_dir)

system_pki_dir = "/etc/pki/entitlement/"
system_rhsm_conf = "/etc/rhsm/rhsm.conf"
system_rhsm_ca = "/etc/rhsm/ca/"


# Validate files exist
if os.path.isdir(system_pki_dir):
    if len (listdir(system_pki_dir)) == 0:
        sys.exit("{} is empty. Aborting.".format(system_pki_dir))

if not os.path.isfile(system_rhsm_conf):
    sys.exit("{} is missing. Aborting.".format(system_rhsm_conf))

if os.path.isdir(system_rhsm_ca):
    if len (listdir(system_rhsm_ca)) == 0:
        sys.exit("{} is empty. Aborting.".format(system_rhsm_ca))



# Copy the files
for file in listdir(system_pki_dir):
    file_full_path = os.path.join(system_pki_dir, file)
    print("Copying {}".format(file_full_path))
    shutil.copy(file_full_path, pki_entitlement_dir)


for file in listdir(system_rhsm_ca):
    file_full_path = os.path.join(system_rhsm_ca, file)
    print("Copying {}".format(file_full_path))
    shutil.copy(file_full_path, rhsm_ca_dir)

print("Copying {}".format(system_rhsm_conf))
shutil.copy(system_rhsm_conf, rhms_conf_dir)


print("Done copying files required for subscription-manager!")
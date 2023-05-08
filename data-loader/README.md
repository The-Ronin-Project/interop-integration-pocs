# data-loader

This project can be used to load some data for experimentation. Early versions resulted in CSV and local JSON files,
while the current standard involves posting data to OCI Buckets that others can access.

# Setup

### To begin with, the following environment variables need to be set on your local system.

_These values can be found by pulling the tenant config for the tenant and EHR that you're trying to connect to._\
LOAD_MRN_SYSTEM\
LOAD_SERVICE_ENDPOINT\
LOAD_AUTH_ENDPOINT

_Depending on if you're running for an Epic or Cerner client you'll also need_\
_Epic_\
LOAD_CLIENT_ID\
LOAD_PRIVATE_KEY\
_Cerner_\
LOAD_ACCOUNT_ID\
LOAD_SECRET


_These values can be found in the HashiCorp Vault, [here](https://vault.devops.projectronin.io:8200/ui/vault/secrets/interop-mirth-connector/show/prod),
under the interop-mirth-connector/prod folder._\
LOAD_OCI_NAMESPACE\
LOAD_OCI_TENANCY_OCID\
LOAD_OCI_USER_OCID\
LOAD_OCI_FINGERPRINT\
LOAD_OCI_REGION_ID\
LOAD_OCI_PRIVATE_KEY

Once the above variables are setup, create a new dataloader via extending `BaseEpicDataLoader` or `BaseCernerDataLoader`.
You'll need to write a `main()` function which will retrieve the data needed, and collate or format is needed before loading it to OCI.
Services in the `service` folder should be be able to be re-used by future data loaders, so avoid putting too much logic there.

If needed, in the resources folder, update _mrns.txt_ with a list of mrns you're interested in loading data for.  If you don't have any and you're interested in PSJ patients, you can pull some from [this](https://docs.google.com/spreadsheets/d/1o9Kl0uZ5rAxra_t1C598CPtVbi_GJdTd2sSnKsm35jI/edit#gid=490983879) Google sheet.

Last but not least, click the run button next to ```fun main()``` in your data loader, then just sit back and watch the magic happen!
# Verifying Data

The following steps can be used to verify that the data was properly loaded into the OCI data lake.

1. Access [Oracle Cloud](https://cloud.oracle.com)
2. Set the `Cloud Account Name` to `projectronin` and select Next.
3. Choose `okta` as the `Identity Provider` and select Continue, logging into Okta if needed.
4. In the Search box at the top, type in `Buckets` and select it from the `Services` heading
5. Ensure the `Compartment` on the left is set to `prod_data-platform`. If it's not already set, it can be found
   under `projectronin > prod`.
6. Select the `prod-experimentation` bucket.
7. In the Objects view, you should see a folder for the tenant you pulled in the format `[TENANT]_data_exploration`,
   such as `psj_data_exploration`.
8. Within this folder you will see all of the resource types that have been pulled in. Select the one you are interested
   in verifying.
9. Within each resource, there should be a folder with the timestamp (as milliseconds since epoch) at the beginning of
   the load.
10. Verify the contents of the appropriate timestamp based off the `Last Modified` times and number of files.

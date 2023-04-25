# data-loader

This project can be used to load some data for experimentation. Early versions resulted in CSV and local JSON files,
while the current standard involves posting data to OCI Buckets that others can access.

# Setup

TBD

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

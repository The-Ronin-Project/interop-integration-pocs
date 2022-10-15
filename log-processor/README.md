# log-processor

Provides some utilities for processing DataDog logs.

## [ValidationLogProcessor](src/main/kotlin/com/projectronin/interops/log/processor/validation/ValidationLogProcessor.kt)

Running the main function with a downloaded CSV from DataDog loaded in as `logs.csv` in the `src/main/resources` folder
will produce output like the following:

```
Total validation entires: 741
741: ERROR REQ_FIELD: value is a required element @ Patient.identifier[0].value

Total non-validation entires: 123
61: [message1]
61: [message2]
1: [message3]
```

# Amida-Hapi

This is a basic restful Hapi server using Hapi and set up against dstu2.

## Application Settings

in /src/main/resources copy application.yml.example to application.yml. Set enableAuth to true if you want to use SMART on FHIR

## Build

``./gradlew build``

## Build Docker

first build

``docker build -t amida-hapi .``

## Create an instance from the image

By default the image will list on 8080  with a root path of /fhir

``docker run --name my-hapi -p 8080:8080 amida-hapi``

you should then be able to go to

``http://localhost:8080/fhir/metadata?_format=json``

to pull down the metadata statement.

This project by default populates with an initial sert of data.  If you wish to override this mount a directory at /var/hapi/init with the resources you wish to populate with.

Resources will be created in alphabetical order of the file.  references for objects must be valid in relation to the dataset.  i.e. if a procuedure references a "Patient/1" then there must be a patient with the id of "Patient/1" that is created before the procedure.

## Smart on FHIR

To use SMART on FHIR a docker compose file will deploy the hapi fhir and keycloak server with

``docker compose up -d``

You can create a client like this

``http://localhost:8080/registerClient/newClient``

And then use that client to query patient data like this

``http://localhost:8080/start/newClient/1``

Which will get the patient data for the patient with the ID of 1

If you set enableAuth to true, you can only access the patient info through those links

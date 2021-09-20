# Amida-Hapi

This is a basic restful Hapi server using Hapi and set up against dstu2.  

## Build

``./gradlew build``

## Build Docker

first build

``docker build -t amida-hapi .`` 

## Create an instance from the image
By default the image will list on 8080  with a root path of /fhir

``docker run --name my-hapi -p 8080:8080 amida-hapi``

you should then be able to go to 

http://localhost:8087/fhir/metadata?_format=json 

to pull down the metadata statement.  

This project by default populates with an initial sert of data.  If you wish to override this mount a directory at /var/hapi/init with the resources you wish to populate with.

Resources will be created in alphabetical order of the file.  references for objects must be valid in relation to the dataset.  i.e. if a procuedure references a "Patient/1" then there must be a patient with the id of "Patient/1" that is created before the procedure.
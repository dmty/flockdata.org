-- noinspection SqlNoDataSourceInspectionForFile
CREATE TABLE TEST (
    ID int GENERATED BY DEFAULT AS IDENTITY (START WITH 1, INCREMENT BY 1) NOT NULL,
    FIRSTNAME varchar(25) NOT NULL,
    LASTNAME varchar(25) NOT NULL,
    SURNAME varchar(23) NOT NULL,
    SECURITY_ID INT NOT NULL
);
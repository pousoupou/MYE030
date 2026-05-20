# MYE030
___
## Prerequisites
- MySQL 8.0 or newer
- Java JDK 26 or newer
- JavaFX
- JDBC Driver
- 7-zip or any program to extract .7z files
- [Data](https://drive.google.com/file/d/1GbEqrrsulajgEjfKZX1lo171Ax2aF6pP/view?usp=drive_link) to transform or load in the DB
> Extract the downloaded data.7z in `src/main/resources` directory
___
### Create and Populate the Database
From `src/main/resources/DDL` directory run
1. `db_create.sql`
2. `load_csv.sql`
3. `views.sql`
> Due to MySQL specifications, _or more accurately constraints_, inside `load_csv.sql` you have to change the absolute path to the project accordingly
___
### If you want to generate the data yourself
Compile with `javac` and run with `java` the `DataExtractAndTransform.java` inside `src/main/java/gr/uoi/cs/mye030/etl/`

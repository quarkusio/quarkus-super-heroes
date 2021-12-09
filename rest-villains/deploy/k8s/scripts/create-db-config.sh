#!/bin/bash

config_file=deploy/k8s/db-config.yml
init_db_dir=infrastructure/db-init
init_db_file=${init_db_dir}/initialize-database.sql
init_tables_file=${init_db_dir}/initialize-tables.sql

# Need to generate a db-config.yml and append the db stuff from the infrastructure/db-init folder into it

echo "apiVersion: v1" > $config_file
echo "kind: ConfigMap" >> $config_file
echo "metadata:" >> $config_file
echo "  name: villains-db-init" >> $config_file
echo "  labels:" >> $config_file
echo "    app: villains-db" >> $config_file
echo "data:" >> $config_file
echo "  init-db.sql: |-" >> $config_file
awk '{print "    " $0}' $init_tables_file >> $config_file

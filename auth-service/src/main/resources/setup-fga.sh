docker run -p 8088:8080 -p 8089:8081 -p 3001:3000 openfga/openfga run
export FGA_API_HOST=localhost:8088
export STORE_ID=$(curl -X POST $FGA_API_HOST/stores   -H "content-type: application/json"   -d '{"name": "Superheroes"}' | jq -r .id)
curl -X POST $FGA_API_HOST/stores/$STORE_ID/authorization-models   -H "content-type: application/json"   -d '@./openfga-model.json'
curl -X POST $FGA_API_HOST/stores/$STORE_ID/write \
  -H "content-type: application/json" \
  -d '@./openfga-tuples.json'

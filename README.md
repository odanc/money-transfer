# money-transfer
REST web-service for transfering money implemented in Scala using functional style

## Usage
1. `sbt run` for starting application (it will use port 8080)
2. `sbt test` for running tests
3. `sbt assembly` for building an executable single jar-file

## Endpoints

##### Retrieve all accounts
###### Request `GET` to `/api/accounts`
###### Response `HTTP 200 OK` with body, e.g.:
```json
[
    {
        "id": "dca85eeb-b03e-4ad3-880f-8c38f28931cb",
        "name": "John Doe",
        "amount": 0
    },
    {
        "id": "c9c4517e-9dac-4fb4-aacb-ec86e3befd0d",
        "name": "Jane Doe",
        "amount": 50
    }
]
```

##### Retrieve an account by id
###### Request `GET` to `/api/accounts/{id}`
###### Response `HTTP 200 OK` with body, e.g.:
```json
{
    "id": "dca85eeb-b03e-4ad3-880f-8c38f28931cb",
    "name": "John Doe",
    "amount": 0
}
```

##### Create an account
###### Request `POST` to `/api/accounts` with body, e.g.:
```json
{
	"name": "Mister X",
	"amount": 20.19
}
```
###### Response `HTTP 201 CREATED` with body, e.g.:
```json
{
    "id": "dca85eeb-b03e-4ad3-880f-8c38f28931cb",
    "name": "Mister X",
    "amount": 20.19
}
```

##### Transfer amount between accounts
###### Request `POST` to `/api/transactions` with body, e.g.:
```json
{
	"from": "dca85eeb-b03e-4ad3-880f-8c38f28931cb",
	"to": "a3af0bda-4fc8-4e04-988a-c6ad992a2835",
	"amount": 50
}
```
###### Response `HTTP 200 OK` with body, e.g.:
```json
{
	"from": "dca85eeb-b03e-4ad3-880f-8c38f28931cb",
	"to": "a3af0bda-4fc8-4e04-988a-c6ad992a2835",
	"amount": 50
}
```

##### Errors
Business errors (not technical ones, e.g. parsing errors, they are processed by underlying
http-library) are returned as a json response with descriptive `message`, e.g.:
```json
{
    "message": "Account dca85eeb-b03e-4ad3-880f-8c38f28931cb doesn't have enough amount"
}
```
or
```json
[
    {
        "message": "Account dca85eeb-b03e-4ad3-880f-8c38f28931cb doesn't exist"
    },
    {
        "message": "Account a3af0bda-4fc8-4e04-988a-c6ad992a2835 doesn't exist"
    }
]
```
Such responses are marked with corresponding 4xx `HTTP` status codes.

## Intro
Upon the start application will generate 2 random accounts for convinience.
They can be retrieved by requesting `GET` `/api/accounts`.
Domain models are remained as simple as possible for clarity purposes
and no "real world" rules are applied: no strict checks for money transfers possibility,
no constraints for amount values, no transactions scheduling or states.
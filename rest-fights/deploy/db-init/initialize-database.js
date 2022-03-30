db.createUser(
    {
        user: "superfight",
        pwd: "superfight",
        roles: [
            { role: "readWrite", db: "fights" }
        ]
    }
)

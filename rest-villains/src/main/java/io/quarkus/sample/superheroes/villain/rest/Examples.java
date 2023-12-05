package io.quarkus.sample.superheroes.villain.rest;

final class Examples {
  private Examples() {

  }

	static final String VALID_EXAMPLE_VILLAIN = """
    {
      "id": 1,
      "name": "Darth Vader",
      "level": 5,
      "picture": "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/anakin-skywalker--8429855148488965479.jpg",
      "powers": "Uses light sabre, dark side of the force"
    }
    """;

	static final String VALID_EXAMPLE_VILLAIN_TO_CREATE = """
    {
      "name": "Darth Vader",
      "level": 5,
      "picture": "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/anakin-skywalker--8429855148488965479.jpg",
      "powers": "Uses light sabre, dark side of the force"
    }
		""";

	static final String VALID_EXAMPLE_VILLAIN_LIST = "[" + VALID_EXAMPLE_VILLAIN + "]";
}

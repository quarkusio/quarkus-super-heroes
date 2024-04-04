package io.quarkus.sample.superheroes.hero.rest;

final class Examples {
  private Examples() {

  }
	static final String VALID_EXAMPLE_HERO = """
		{
			"id": 1,
			"name": "Luke Skywalker",
			"level": 10,
			"picture": "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/luke-skywalker-2563509063968639219.jpg",
			"powers": "Uses light sabre, The force"
		}
		""";

	static final String VALID_EXAMPLE_HERO_TO_CREATE = """
    {
			"name": "Luke Skywalker",
			"level": 10,
			"picture": "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/luke-skywalker-2563509063968639219.jpg",
			"powers": "Uses light sabre, The force"
		}
		""";

	static final String VALID_EXAMPLE_HERO_LIST = "[" + VALID_EXAMPLE_HERO + "]";
}

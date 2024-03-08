package io.quarkus.sample.superheroes.narration.rest;

final class Examples {
  private Examples() {

  }

	static final String EXAMPLE_FIGHT = """
    {
      "winnerName": "Chewbacca",
      "winnerLevel": 5,
      "winnerPicture": "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/chewbacca--684239239428094811.jpg",
      "winnerPowers": "Big, hairy, strong",
      "winnerTeam": "heroes",
      "loserName": "Wanderer",
      "loserLevel": 3,
      "loserPicture": "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/wanderer-300775911119209178.jpg",
      "loserPowers": "Not strong",
      "location": {
        "name": "Gotham City",
        "description": "An American city rife with corruption and crime, the home of its iconic protector Batman."
      }
    }
    """;

	static final String EXAMPLE_NARRATION = """
    In the dark, shadowy alleys of Gotham City, a fierce battle unfolded between two formidable opponents. The towering, imposing figure known as Chewbacca faced off against the agile and mysterious Wanderer. The air crackled with tension as the two clashed, their powers contrasting starkly against each other.
    
    Chewbacca's sheer strength and ferocity were unmatched as he unleashed a flurry of powerful blows, his massive frame dominating the scene. In contrast, Wanderer's quick reflexes and cunning tactics kept him in the fight, dodging and weaving with grace and precision. The clash of styles made for a mesmerizing spectacle as the combatants danced around each other.
    
    Despite Wanderer's best efforts, it was clear that Chewbacca's overwhelming power was too much to handle. With a mighty roar, Chewbacca delivered a final, decisive blow that sent Wanderer crashing to the ground. The hero stood victorious, his victory a testament to his incredible strength and unwavering determination.
    
    As the dust settled and the citizens of Gotham City breathed a collective sigh of relief, Chewbacca stood tall, a beacon of hope in a city plagued by darkness. The defeated Wanderer, though vanquished, would always be remembered for his valiant effort in the face of insurmountable odds. And so, the tale of their epic clash would be etched into the annals of superhero lore, a testament to the eternal struggle between good and evil.
    """;

  static final String EXAMPLE_FIGHT_IMAGE = """
     {
       "imageUrl": "https://oaidalleapiprodscus.blob.core.windows.net/private/org-GdpIlMzhC20CU6Tcnu4Qe6B4/user-ljypWMQk95mvH7oFhiClsWLf/img-2YLTLfddXEma581WvHxlOeM3.png?st=2024-03-08T17%3A24%3A42Z&se=2024-03-08T19%3A24%3A42Z&sp=r&sv=2021-08-06&sr=b&rscd=inline&rsct=image/png&skoid=6aaadede-4fb3-4698-a8f6-684d7786b067&sktid=a48cca56-e6da-484e-a814-9c849652bcb3&skt=2024-03-08T18%3A14%3A53Z&ske=2024-03-09T18%3A14%3A53Z&sks=b&skv=2021-08-06&sig=Lq0orGMjEy4JOv/xF6guJqx8c4Q7bRrKeAUNAyW2pbw%3D",
       "imageNarration": "In a dark, shadowy urban setting, a dramatic battle unfolds between two formidable opponents. A towering figure characterized by a brown furry texture, huge physical stature, and excessive strength battles against a sleek, agile figure known as the Wanderer. This dark alley is filled with the electric tension of an intense battle between contrasting strengths - raw power and tactical agility. The viewers are entertained by the mesmerizing interplay between power and precision, the raw strength of the furry figure continually trumps the agile maneuvers of the Wanderer. Triumph takes the form of the towering, hirsute protagonist leaving the agile opponent sprawled on the ground. Embodying hope, the furry giant stands victorious in a city shadowed by uncertainty. The tale of this epic struggle paints an engaging portrait of the age-old battle between opposing forces."
     }
     """;
}

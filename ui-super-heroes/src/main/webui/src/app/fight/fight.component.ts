import {Component, OnInit} from '@angular/core';
import {Fight, Fighters, FightService} from '../shared';
import {FightLocation} from "../shared/model/fightLocation";
import {FightRequest} from "../shared/model/fightRequest";

@Component({
  selector: 'hero-fight',
  templateUrl: './fight.component.html'
})
export class FightComponent implements OnInit {
  fighters: Fighters = new Fighters();
  location: FightLocation;
  wonFight: Fight;
  winner: String;
  narration: string;

  constructor(private fightService: FightService) {
  }

  ngOnInit() {
    this.newFighters();
  }

  fight() {
    let fightRequest = new FightRequest(this.fighters.hero, this.fighters.villain, this.location)

    this.fightService.apiFightsPost(fightRequest).subscribe(fight => {
          this.fightService.onNewFight(fight);
          this.winner = fight.winnerName;
          this.wonFight = fight;
          this.narration = "";
          this.location = fight.location;
        }
    );
  }

  narrate() {
    this.narration = null;
    this.fightService.apiNarrateFightPost(this.wonFight).subscribe(
        narration => {
          this.narration = narration;
          this.fightService.onNewFightNarration(this.narration);
        }
    );
  }

  newFighters() {
    this.winner = null;
    this.fightService.apiFightsRandomfightersGet().subscribe(fighters => this.fighters = fighters);
  }

  newLocation() {
    this.location = null;
    this.fightService.apiFightsRandomLocationGet().subscribe(location => this.location = location);
  }
}

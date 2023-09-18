import {Component, OnInit} from '@angular/core';
import {Fight, Fighters, FightService} from '../shared';

@Component({
  selector: 'hero-fight',
  templateUrl: './fight.component.html'
})
export class FightComponent implements OnInit {

  fighters: Fighters = new Fighters();
  wonFight: Fight;
  winner: String;
  narration: string;

  constructor(private fightService: FightService) {
  }

  ngOnInit() {
    this.newFighters();
  }

  fight() {
    this.fightService.apiFightsPost(this.fighters).subscribe(
      fight => {
        this.fightService.onNewFight(fight);
        this.winner = fight.winnerName;
        this.wonFight = fight;
        this.narration = "";
      }
    );
  }

  narrate() {
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
}

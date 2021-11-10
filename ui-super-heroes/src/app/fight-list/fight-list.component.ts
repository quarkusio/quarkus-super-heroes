import {Component, OnInit} from '@angular/core';
import { Fight, FightService } from '../shared';
import {MatTableDataSource} from "@angular/material/table";

@Component({
  selector: 'hero-fight-list',
  templateUrl: './fight-list.component.html',
  styles: []
})
export class FightListComponent implements OnInit {

  dataSource: MatTableDataSource < Fight > ;
  displayedColumns: string[] = ['id', 'fightDate', 'winnerName', 'loserName'];

  constructor(private fightService: FightService) {
    this.dataSource = new MatTableDataSource<Fight>();
    fightService.emitter.subscribe(fight => {
      const data = this.dataSource.data;
      data.unshift(fight);
      this.dataSource.data = data;
    })
  }

  ngOnInit() {
    this.fightService.apiFightsGet().subscribe(fights => {
      this.dataSource.data = fights.reverse();
    });
  }
}

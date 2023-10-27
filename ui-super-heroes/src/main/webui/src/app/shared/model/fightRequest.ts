import {Hero} from './hero';
import {Villain} from './villain';
import {FightLocation} from "./fightLocation";

/**
 * A fight between one hero and one villain
 */
export class FightRequest {
  constructor(
    public hero?: Hero,
    public villain?: Villain,
    public location?: FightLocation
  ) {
  }
}

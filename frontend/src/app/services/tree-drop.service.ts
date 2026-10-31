import { Injectable } from '@angular/core';
import { CdkDropList } from '@angular/cdk/drag-drop';
import { Subject } from 'rxjs';

/**
 * Keeps track of every drop list rendered by the recursive tree so each one
 * can be connected to all the others (nested cdkDropLists are not connected
 * automatically).
 */
@Injectable({ providedIn: 'root' })
export class TreeDropService {
  private readonly lists: CdkDropList[] = [];
  readonly changed$ = new Subject<void>();

  register(list: CdkDropList): void {
    if (!this.lists.includes(list)) {
      this.lists.push(list);
      this.changed$.next();
    }
  }

  unregister(list: CdkDropList): void {
    const idx = this.lists.indexOf(list);
    if (idx >= 0) {
      this.lists.splice(idx, 1);
      this.changed$.next();
    }
  }

  getConnectedLists(): readonly CdkDropList[] {
    return this.lists;
  }
}

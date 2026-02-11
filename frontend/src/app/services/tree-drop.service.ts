import { Injectable } from '@angular/core';
import { CdkDropList } from '@angular/cdk/drag-drop';
import { Subject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class TreeDropService {
  private lists: CdkDropList[] = [];
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

  getConnectedLists(): CdkDropList[] {
    return this.lists;
  }
}

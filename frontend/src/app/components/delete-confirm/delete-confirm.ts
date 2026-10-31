import {
  Component,
  ElementRef,
  afterNextRender,
  computed,
  input,
  output,
  viewChild,
} from '@angular/core';
import { TreeNode } from '../../models/tree.model';

@Component({
  selector: 'app-delete-confirm',
  templateUrl: './delete-confirm.html',
  styleUrl: './delete-confirm.scss',
})
export class DeleteConfirm {
  readonly node = input.required<TreeNode>();

  readonly confirmed = output<void>();
  readonly cancelled = output<void>();

  /** Names of the node and every descendant, depth first. */
  readonly affectedNodes = computed(() => {
    const names: string[] = [];
    const collect = (n: TreeNode): void => {
      names.push(n.name);
      (n.children ?? []).forEach(collect);
    };
    collect(this.node());
    return names;
  });

  readonly childCount = computed(() => this.affectedNodes().length - 1);

  private readonly cancelButton = viewChild.required<ElementRef<HTMLButtonElement>>('cancelButton');

  constructor() {
    // focus inside the dialog so Escape reaches the overlay's key handler
    afterNextRender(() => this.cancelButton().nativeElement.focus());
  }

  onOverlayClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.cancelled.emit();
    }
  }
}

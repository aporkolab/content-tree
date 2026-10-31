import {
  AfterViewInit,
  Component,
  DestroyRef,
  computed,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CdkDragDrop, CdkDropList, DragDropModule } from '@angular/cdk/drag-drop';
import { TreeNode } from '../../models/tree.model';
import { TreeDropService } from '../../services/tree-drop.service';

@Component({
  selector: 'app-tree-node',
  imports: [DragDropModule],
  templateUrl: './tree-node.html',
  styleUrl: './tree-node.scss',
})
export class TreeNodeComponent implements AfterViewInit {
  readonly node = input.required<TreeNode>();
  readonly selectedId = input<number | null>(null);
  readonly level = input(0);

  readonly nodeSelect = output<TreeNode>();
  readonly nodeCreate = output<number>();
  readonly nodeEdit = output<TreeNode>();
  readonly nodeDelete = output<TreeNode>();
  readonly nodeDrop = output<CdkDragDrop<TreeNode>>();

  private readonly rowDropList = viewChild.required<CdkDropList<TreeNode>>('rowDropList');
  private readonly childDropList = viewChild.required<CdkDropList<TreeNode>>('childDropList');

  private readonly dropService = inject(TreeDropService);
  private readonly destroyRef = inject(DestroyRef);

  readonly expanded = signal(true);
  readonly isSelected = computed(() => this.node().id === this.selectedId());
  readonly isRoot = computed(() => this.node().parentId === null);
  readonly hasChildren = computed(() => (this.node().children?.length ?? 0) > 0);

  ngAfterViewInit(): void {
    const row = this.rowDropList();
    const children = this.childDropList();
    this.dropService.register(row);
    this.dropService.register(children);

    this.dropService.changed$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.connectLists());

    this.destroyRef.onDestroy(() => {
      this.dropService.unregister(row);
      this.dropService.unregister(children);
    });

    // run once more after sibling nodes rendered in the same pass have registered
    setTimeout(() => this.connectLists());
  }

  toggleExpand(event: Event): void {
    event.stopPropagation();
    event.preventDefault();
    this.expanded.update((v) => !v);
  }

  select(event?: Event): void {
    event?.preventDefault();
    this.nodeSelect.emit(this.node());
  }

  private connectLists(): void {
    const all = this.dropService.getConnectedLists();
    const row = this.rowDropList();
    const children = this.childDropList();
    row.connectedTo = all.filter((l) => l !== row);
    children.connectedTo = all.filter((l) => l !== children);
  }
}

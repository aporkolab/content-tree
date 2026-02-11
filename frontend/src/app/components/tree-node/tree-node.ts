import {
  Component,
  Input,
  Output,
  EventEmitter,
  ChangeDetectionStrategy,
  ViewChild,
  AfterViewInit,
  OnDestroy,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { CdkDragDrop, CdkDropList, DragDropModule } from '@angular/cdk/drag-drop';
import { Subscription } from 'rxjs';
import { TreeNode } from '../../models/tree.model';
import { TreeDropService } from '../../services/tree-drop.service';

@Component({
  selector: 'app-tree-node',
  imports: [CommonModule, DragDropModule],
  templateUrl: './tree-node.html',
  styleUrl: './tree-node.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TreeNodeComponent implements AfterViewInit, OnDestroy {
  @Input({ required: true }) node!: TreeNode;
  @Input() selectedId: number | null = null;
  @Input() level = 0;

  @Output() nodeSelect = new EventEmitter<TreeNode>();
  @Output() nodeCreate = new EventEmitter<number>();
  @Output() nodeEdit = new EventEmitter<TreeNode>();
  @Output() nodeDelete = new EventEmitter<TreeNode>();
  @Output() nodeDrop = new EventEmitter<CdkDragDrop<TreeNode>>();

  @ViewChild('rowDropList', { static: true }) rowDropList!: CdkDropList;
  @ViewChild('childDropList', { static: true }) childDropList!: CdkDropList;

  private readonly dropService = inject(TreeDropService);
  private sub?: Subscription;

  expanded = true;

  get isSelected(): boolean {
    return this.node.id === this.selectedId;
  }

  get hasChildren(): boolean {
    return this.node.children?.length > 0;
  }

  ngAfterViewInit(): void {
    this.dropService.register(this.rowDropList);
    this.dropService.register(this.childDropList);

    this.sub = this.dropService.changed$.subscribe(() => {
      this.connectLists();
    });

    setTimeout(() => this.connectLists());
  }

  ngOnDestroy(): void {
    this.dropService.unregister(this.rowDropList);
    this.dropService.unregister(this.childDropList);
    this.sub?.unsubscribe();
  }

  toggleExpand(event: Event): void {
    event.stopPropagation();
    this.expanded = !this.expanded;
  }

  private connectLists(): void {
    const all = this.dropService.getConnectedLists();
    this.rowDropList.connectedTo = all.filter(l => l !== this.rowDropList);
    this.childDropList.connectedTo = all.filter(l => l !== this.childDropList);
  }
}

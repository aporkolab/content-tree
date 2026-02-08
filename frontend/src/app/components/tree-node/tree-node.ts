import {
  Component,
  Input,
  Output,
  EventEmitter,
  ChangeDetectionStrategy,
  OnDestroy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { TreeNode } from '../../models/tree.model';

@Component({
  selector: 'app-tree-node',
  standalone: true,
  imports: [CommonModule, DragDropModule],
  templateUrl: './tree-node.html',
  styleUrl: './tree-node.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TreeNodeComponent {
  @Input({ required: true }) node!: TreeNode;
  @Input() selectedId: number | null = null;
  @Input() level = 0;

  @Output() nodeSelect = new EventEmitter<TreeNode>();
  @Output() nodeCreate = new EventEmitter<number>();
  @Output() nodeEdit = new EventEmitter<TreeNode>();
  @Output() nodeDelete = new EventEmitter<TreeNode>();
  @Output() nodeDrop = new EventEmitter<CdkDragDrop<TreeNode>>();

  expanded = true;

  get isSelected(): boolean {
    return this.node.id === this.selectedId;
  }

  get hasChildren(): boolean {
    return this.node.children?.length > 0;
  }

  toggleExpand(event: Event): void {
    event.stopPropagation();
    this.expanded = !this.expanded;
  }
}

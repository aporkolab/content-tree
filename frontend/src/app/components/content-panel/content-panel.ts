import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TreeNode } from '../../models/tree.model';

@Component({
  selector: 'app-content-panel',
  imports: [CommonModule],
  templateUrl: './content-panel.html',
  styleUrl: './content-panel.scss',
})
export class ContentPanel {
  @Input({ required: true }) node!: TreeNode;

  @Output() addChild = new EventEmitter<number>();
  @Output() edit = new EventEmitter<TreeNode>();
  @Output() delete = new EventEmitter<TreeNode>();

  get isRoot(): boolean {
    return this.node.parentId === null;
  }
}

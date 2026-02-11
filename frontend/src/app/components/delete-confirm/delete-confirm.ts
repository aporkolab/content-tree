import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TreeNode } from '../../models/tree.model';

@Component({
  selector: 'app-delete-confirm',
  imports: [CommonModule],
  templateUrl: './delete-confirm.html',
  styleUrl: './delete-confirm.scss',
})
export class DeleteConfirm {
  @Input({ required: true }) node!: TreeNode;

  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();

  get childCount(): number {
    return this.countChildren(this.node);
  }

  get affectedNodes(): string[] {
    const names: string[] = [];
    this.collectNames(this.node, names);
    return names;
  }

  private countChildren(node: TreeNode): number {
    if (!node.children) return 0;
    let count = node.children.length;
    for (const child of node.children) {
      count += this.countChildren(child);
    }
    return count;
  }

  private collectNames(node: TreeNode, names: string[]): void {
    names.push(node.name);
    if (node.children) {
      for (const child of node.children) {
        this.collectNames(child, names);
      }
    }
  }

  onOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('dialog-overlay')) {
      this.cancel.emit();
    }
  }
}

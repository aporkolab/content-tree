import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TreeNode, TreeNodeRequest } from '../../models/tree.model';

@Component({
  selector: 'app-node-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './node-dialog.html',
  styleUrl: './node-dialog.scss',
})
export class NodeDialog implements OnInit {
  @Input() mode: 'create' | 'edit' = 'create';
  @Input() parentId: number | null = null;
  @Input() node: TreeNode | null = null;

  @Output() save = new EventEmitter<TreeNodeRequest>();
  @Output() cancel = new EventEmitter<void>();

  name = '';
  content = '';
  submitted = false;

  get title(): string {
    return this.mode === 'create' ? 'Create Node' : 'Edit Node';
  }

  ngOnInit(): void {
    if (this.mode === 'edit' && this.node) {
      this.name = this.node.name;
      this.content = this.node.content;
    }
  }

  onSubmit(): void {
    this.submitted = true;
    if (!this.name.trim() || !this.content.trim()) {
      return;
    }

    const request: TreeNodeRequest = {
      name: this.name.trim(),
      content: this.content.trim(),
      parentId: this.mode === 'create' ? this.parentId : this.node?.parentId ?? null,
    };

    this.save.emit(request);
  }

  onCancel(): void {
    this.cancel.emit();
  }

  onOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('dialog-overlay')) {
      this.onCancel();
    }
  }
}

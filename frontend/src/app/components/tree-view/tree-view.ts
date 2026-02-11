import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { BehaviorSubject, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { TreeNode, TreeNodeRequest } from '../../models/tree.model';
import { TreeService } from '../../services/tree.service';
import { TreeNodeComponent } from '../tree-node/tree-node';
import { ContentPanel } from '../content-panel/content-panel';
import { NodeDialog } from '../node-dialog/node-dialog';
import { DeleteConfirm } from '../delete-confirm/delete-confirm';

@Component({
  selector: 'app-tree-view',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TreeNodeComponent,
    ContentPanel,
    NodeDialog,
    DeleteConfirm,
  ],
  templateUrl: './tree-view.html',
  styleUrl: './tree-view.scss',
})
export class TreeView implements OnInit {
  private readonly treeService = inject(TreeService);
  private readonly cdr = inject(ChangeDetectorRef);

  tree$ = new BehaviorSubject<TreeNode | null>(null);
  selectedNode: TreeNode | null = null;
  searchQuery = '';
  loading = false;

  // dialog state
  showNodeDialog = false;
  dialogMode: 'create' | 'edit' = 'create';
  dialogParentId: number | null = null;
  editingNode: TreeNode | null = null;

  showDeleteDialog = false;
  deletingNode: TreeNode | null = null;

  private searchSubject = new Subject<string>();

  ngOnInit(): void {
    this.loadTree();

    this.searchSubject
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((query) =>
          query ? this.treeService.searchTree(query) : this.treeService.getTree()
        )
      )
      .subscribe((tree) => {
        this.tree$.next(tree);
        this.cdr.markForCheck();
      });
  }

  loadTree(): void {
    this.loading = true;
    this.treeService.getTree().subscribe({
      next: (tree) => {
        this.tree$.next(tree);
        this.loading = false;
        // console.log('loaded tree:', tree);
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  onSearch(query: string): void {
    this.searchSubject.next(query);
  }

  onNodeSelect(node: TreeNode): void {
    this.selectedNode = node;
  }

  onNodeDrop(event: CdkDragDrop<TreeNode>): void {
    const movedNode = event.item.data as TreeNode;
    const newParent = event.container.data as TreeNode;

    if (movedNode.id !== newParent.id && movedNode.parentId !== newParent.id) {
      this.treeService.moveNode(movedNode.id, newParent.id).subscribe({
        next: () => this.loadTree(),
        error: (err) => {
          console.error('Failed to move node:', err);
          this.loadTree();
        },
      });
    }
  }

  openCreateDialog(parentId: number | null): void {
    this.dialogMode = 'create';
    this.dialogParentId = parentId;
    this.editingNode = null;
    this.showNodeDialog = true;
  }

  openEditDialog(node: TreeNode): void {
    this.dialogMode = 'edit';
    this.editingNode = node;
    this.dialogParentId = node.parentId;
    this.showNodeDialog = true;
  }

  openDeleteConfirm(node: TreeNode): void {
    this.deletingNode = node;
    this.showDeleteDialog = true;
  }

  onDialogSave(request: TreeNodeRequest): void {
    if (this.dialogMode === 'create') {
      this.treeService.createNode(request).subscribe({
        next: () => {
          this.showNodeDialog = false;
          this.loadTree();
        },
        error: (err) => {
          console.error('Failed to create node:', err);
          this.showNodeDialog = false;
          alert(err.error || 'Failed to create node');
        },
      });
    } else if (this.editingNode) {
      this.treeService.updateNode(this.editingNode.id, request).subscribe({
        next: () => {
          this.showNodeDialog = false;
          this.selectedNode = null;
          this.loadTree();
        },
        error: (err) => {
          console.error('Failed to update node:', err);
          this.showNodeDialog = false;
          alert(err.error || 'Failed to update node');
        },
      });
    }
  }

  onDialogCancel(): void {
    this.showNodeDialog = false;
  }

  onDeleteConfirm(): void {
    if (this.deletingNode) {
      this.treeService.deleteNode(this.deletingNode.id).subscribe({
        next: () => {
          this.showDeleteDialog = false;
          if (this.selectedNode?.id === this.deletingNode?.id) {
            this.selectedNode = null;
          }
          this.deletingNode = null;
          this.loadTree();
        },
        error: (err) => {
          console.error('Failed to delete node:', err);
          alert('Failed to delete node. It may be the root node with children.');
          this.showDeleteDialog = false;
        },
      });
    }
  }

  onDeleteCancel(): void {
    this.showDeleteDialog = false;
    this.deletingNode = null;
  }
}

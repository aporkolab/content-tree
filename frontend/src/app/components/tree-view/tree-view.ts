import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BehaviorSubject } from 'rxjs';
import { TreeNode } from '../../models/tree.model';
import { TreeService } from '../../services/tree.service';
import { TreeNodeComponent } from '../tree-node/tree-node';

@Component({
  selector: 'app-tree-view',
  standalone: true,
  imports: [CommonModule, TreeNodeComponent],
  templateUrl: './tree-view.html',
  styleUrl: './tree-view.scss',
})
export class TreeView implements OnInit {
  private readonly treeService = inject(TreeService);

  tree$ = new BehaviorSubject<TreeNode | null>(null);
  selectedNode: TreeNode | null = null;
  loading = false;

  ngOnInit(): void {
    this.loadTree();
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

  onNodeSelect(node: TreeNode): void {
    this.selectedNode = node;
  }
}

import { Component, computed, input, output } from '@angular/core';
import { TreeNode } from '../../models/tree.model';

@Component({
  selector: 'app-content-panel',
  templateUrl: './content-panel.html',
  styleUrl: './content-panel.scss',
})
export class ContentPanel {
  readonly node = input.required<TreeNode>();

  readonly addChild = output<number>();
  readonly edit = output<TreeNode>();
  readonly delete = output<TreeNode>();

  readonly isRoot = computed(() => this.node().parentId === null);
  readonly children = computed(() => this.node().children ?? []);
}

import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TreeNode } from '../../models/tree.model';

@Component({
  selector: 'app-content-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './content-panel.html',
  styleUrl: './content-panel.scss',
})
export class ContentPanel {
  @Input({ required: true }) node!: TreeNode;
}

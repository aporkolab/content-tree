import { Component, ChangeDetectionStrategy } from '@angular/core';
import { TreeView } from './components/tree-view/tree-view';

@Component({
  selector: 'app-root',
  imports: [TreeView],
  templateUrl: './app.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './app.scss',
})
export class App {}

import { Component } from '@angular/core';
import { TreeView } from './components/tree-view/tree-view';

@Component({
  selector: 'app-root',
  imports: [TreeView],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {}

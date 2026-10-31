import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { Observable, Subject, merge, of } from 'rxjs';
import {
  catchError,
  debounceTime,
  distinctUntilChanged,
  map,
  switchMap,
  tap,
} from 'rxjs/operators';
import { TreeNode, TreeNodeRequest } from '../../models/tree.model';
import { TreeService, findNodeById, toErrorMessage } from '../../services/tree.service';
import { TreeNodeComponent } from '../tree-node/tree-node';
import { ContentPanel } from '../content-panel/content-panel';
import { NodeDialog } from '../node-dialog/node-dialog';
import { DeleteConfirm } from '../delete-confirm/delete-confirm';

export type DialogState =
  | { kind: 'none' }
  | { kind: 'create'; parentId: number | null }
  | { kind: 'edit'; node: TreeNode }
  | { kind: 'delete'; node: TreeNode };

const ERROR_TOAST_MS = 5000;
const SEARCH_DEBOUNCE_MS = 300;

@Component({
  selector: 'app-tree-view',
  imports: [TreeNodeComponent, ContentPanel, NodeDialog, DeleteConfirm],
  templateUrl: './tree-view.html',
  styleUrl: './tree-view.scss',
})
export class TreeView {
  private readonly treeService = inject(TreeService);
  private readonly destroyRef = inject(DestroyRef);

  readonly tree = signal<TreeNode | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal('');
  readonly searchQuery = signal('');
  readonly dialog = signal<DialogState>({ kind: 'none' });

  private readonly selectedId = signal<number | null>(null);
  /** Re-resolved on every reload, so it is never a stale copy (and null once deleted). */
  readonly selectedNode = computed(() => findNodeById(this.tree(), this.selectedId()));

  private readonly search$ = new Subject<string>();
  private readonly reload$ = new Subject<void>();
  private errorTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    // A single pipeline for searches and reloads: switchMap drops stale
    // responses, and reloads keep the active search filter.
    merge(
      this.search$.pipe(debounceTime(SEARCH_DEBOUNCE_MS), distinctUntilChanged()),
      this.reload$.pipe(map(() => this.searchQuery())),
    )
      .pipe(
        switchMap((query) =>
          this.fetchTree(query).pipe(
            catchError((err: unknown) => {
              this.showError(toErrorMessage(err, 'Failed to load tree'));
              return of(this.tree());
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((tree) => {
        this.tree.set(tree);
        this.loading.set(false);
      });

    this.destroyRef.onDestroy(() => clearTimeout(this.errorTimer));
    this.reload();
  }

  private fetchTree(query: string): Observable<TreeNode | null> {
    const trimmed = query.trim();
    return trimmed ? this.treeService.searchTree(trimmed) : this.treeService.getTree();
  }

  reload(): void {
    this.reload$.next();
  }

  onSearch(query: string): void {
    this.searchQuery.set(query);
    this.search$.next(query);
  }

  onNodeSelect(node: TreeNode): void {
    this.selectedId.set(node.id);
  }

  onNodeDrop(event: CdkDragDrop<TreeNode>): void {
    const movedNode = event.item.data as TreeNode;
    const newParent = event.container.data;

    if (movedNode.id === newParent.id || movedNode.parentId === newParent.id) {
      return;
    }
    this.treeService
      .moveNode(movedNode.id, newParent.id)
      .pipe(this.reloadAfter('Failed to move node'))
      .subscribe();
  }

  openCreateDialog(parentId: number | null): void {
    this.dialog.set({ kind: 'create', parentId });
  }

  openEditDialog(node: TreeNode): void {
    this.dialog.set({ kind: 'edit', node });
  }

  openDeleteConfirm(node: TreeNode): void {
    this.dialog.set({ kind: 'delete', node });
  }

  closeDialog(): void {
    this.dialog.set({ kind: 'none' });
  }

  onDialogSave(request: TreeNodeRequest): void {
    const state = this.dialog();
    let request$: Observable<unknown>;
    let failure: string;
    if (state.kind === 'create') {
      request$ = this.treeService.createNode(request);
      failure = 'Failed to create node';
    } else if (state.kind === 'edit') {
      request$ = this.treeService.updateNode(state.node.id, request);
      failure = 'Failed to update node';
    } else {
      return;
    }

    this.closeDialog();
    request$.pipe(this.reloadAfter(failure)).subscribe();
  }

  onDeleteConfirm(): void {
    const state = this.dialog();
    if (state.kind !== 'delete') {
      return;
    }
    this.closeDialog();
    this.treeService
      .deleteNode(state.node.id)
      .pipe(this.reloadAfter('Failed to delete node'))
      .subscribe();
  }

  /** Reloads the tree once the request settles; failures are shown as a toast. */
  private reloadAfter<T>(failure: string) {
    return (source: Observable<T>) =>
      source.pipe(
        tap({
          next: () => this.reload(),
          error: (err: unknown) => {
            this.showError(toErrorMessage(err, failure));
            this.reload();
          },
        }),
        catchError(() => of(null)),
        takeUntilDestroyed(this.destroyRef),
      );
  }

  private showError(message: string): void {
    clearTimeout(this.errorTimer);
    this.errorMessage.set(message);
    this.errorTimer = setTimeout(() => this.errorMessage.set(''), ERROR_TOAST_MS);
  }
}

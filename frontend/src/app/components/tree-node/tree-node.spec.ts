import { TestBed } from '@angular/core/testing';
import { TreeNodeComponent } from './tree-node';
import { TreeNode } from '../../models/tree.model';

const tree: TreeNode = {
  id: 1,
  name: 'Root',
  content: '',
  parentId: null,
  children: [
    { id: 2, name: 'Docs', content: '', parentId: 1, isMatch: false, children: [] },
    { id: 3, name: 'Pics', content: '', parentId: 1, isMatch: true, children: [] },
  ],
};

describe('TreeNodeComponent', () => {
  async function render(selectedId: number | null = null) {
    const fixture = TestBed.createComponent(TreeNodeComponent);
    fixture.componentRef.setInput('node', tree);
    fixture.componentRef.setInput('selectedId', selectedId);
    await fixture.whenStable();
    return fixture;
  }

  const names = (el: HTMLElement) =>
    Array.from(el.querySelectorAll('.node-name')).map((n) => n.textContent?.trim());

  it('renders the subtree recursively and greys out non-matches', async () => {
    const el = (await render()).nativeElement as HTMLElement;

    expect(names(el)).toEqual(['Root', 'Docs', 'Pics']);
    expect(el.querySelectorAll('.tree-node.non-match')).toHaveLength(1);
  });

  it('collapses and expands children', async () => {
    const fixture = await render();
    const el = fixture.nativeElement as HTMLElement;
    const toggle = el.querySelector<HTMLElement>('.expand-toggle')!;

    toggle.click();
    await fixture.whenStable();
    expect(names(el)).toEqual(['Root']);

    toggle.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
    await fixture.whenStable();
    expect(names(el)).toEqual(['Root', 'Docs', 'Pics']);
  });

  it('bubbles selection from nested nodes and highlights the selected one', async () => {
    const fixture = await render(3);
    const el = fixture.nativeElement as HTMLElement;
    const selected: number[] = [];
    fixture.componentInstance.nodeSelect.subscribe((n) => selected.push(n.id));

    el.querySelectorAll<HTMLElement>('.node-name')[1].click();

    expect(selected).toEqual([2]);
    expect(el.querySelector('.tree-node.selected .node-name')?.textContent?.trim()).toBe('Pics');
  });

  it('offers delete only for non-root nodes', async () => {
    const el = (await render()).nativeElement as HTMLElement;

    expect(el.querySelectorAll('.delete-btn')).toHaveLength(2);
  });
});

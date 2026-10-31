import { TestBed } from '@angular/core/testing';
import { ContentPanel } from './content-panel';
import { TreeNode } from '../../models/tree.model';

describe('ContentPanel', () => {
  async function render(node: TreeNode) {
    const fixture = TestBed.createComponent(ContentPanel);
    fixture.componentRef.setInput('node', node);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('marks the root and hides its delete button', async () => {
    const el = await render({
      id: 1,
      name: 'Root',
      content: 'Hello',
      parentId: null,
      children: [],
    });

    expect(el.textContent).toContain('Root Node');
    expect(el.querySelector('.btn-delete')).toBeNull();
  });

  it('shows parent, content and children of a regular node', async () => {
    const el = await render({
      id: 2,
      name: 'Docs',
      content: 'Body text',
      parentId: 1,
      children: [{ id: 3, name: 'Child', content: '', parentId: 2, children: [] }],
    });

    expect(el.textContent).toContain('Parent: 1');
    expect(el.textContent).toContain('Body text');
    expect(el.textContent).toContain('Children (1)');
    expect(el.querySelector('.btn-delete')).not.toBeNull();
  });
});

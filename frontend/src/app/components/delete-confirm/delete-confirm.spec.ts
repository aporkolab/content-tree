import { TestBed } from '@angular/core/testing';
import { DeleteConfirm } from './delete-confirm';
import { TreeNode } from '../../models/tree.model';

const leaf = (id: number, name: string, parentId: number): TreeNode => ({
  id,
  name,
  content: '',
  parentId,
  children: [],
});

describe('DeleteConfirm', () => {
  async function render(node: TreeNode) {
    const fixture = TestBed.createComponent(DeleteConfirm);
    fixture.componentRef.setInput('node', node);
    await fixture.whenStable();
    return fixture;
  }

  it('lists the node and all of its descendants', async () => {
    const node: TreeNode = {
      ...leaf(2, 'Docs', 1),
      children: [{ ...leaf(3, 'A', 2), children: [leaf(4, 'A1', 3)] }, leaf(5, 'B', 2)],
    };
    const fixture = await render(node);

    expect(fixture.componentInstance.childCount()).toBe(3);
    const items = Array.from(
      fixture.nativeElement.querySelectorAll('li') as NodeListOf<HTMLElement>,
    ).map((li) => li.textContent?.trim());
    expect(items).toEqual(['Docs', 'A', 'A1', 'B']);
    expect(fixture.nativeElement.textContent).toContain('also delete 3 child');
  });

  it('shows no subtree warning for leaves', async () => {
    const fixture = await render(leaf(2, 'Leaf', 1));

    expect(fixture.nativeElement.querySelector('.affected-warning')).toBeNull();
  });

  it('emits confirmed and cancelled', async () => {
    const fixture = await render(leaf(2, 'Leaf', 1));
    const events: string[] = [];
    fixture.componentInstance.confirmed.subscribe(() => events.push('confirm'));
    fixture.componentInstance.cancelled.subscribe(() => events.push('cancel'));

    (fixture.nativeElement.querySelector('.btn-danger') as HTMLButtonElement).click();
    (fixture.nativeElement.querySelector('.btn-secondary') as HTMLButtonElement).click();

    expect(events).toEqual(['confirm', 'cancel']);
  });
});

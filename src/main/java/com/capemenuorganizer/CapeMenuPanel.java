package com.capemenuorganizer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import net.runelite.client.ui.PluginPanel;

/** Swing state belongs to the EDT; saved changes are passed out as independent copies. */
final class CapeMenuPanel extends PluginPanel
{
	private final LayoutCodec codec;
	private final JTree tree = new JTree();
	private final JComboBox<String> destinations = new JComboBox<>();
	private final JTextArea status = text("");
	private final JPanel controls = new JPanel(new GridLayout(0, 2, 4, 4));
	private final JPanel visibility = new JPanel();
	private final List<JButton> editButtons = new ArrayList<>();
	private MenuLayout layout = new MenuLayout();
	private Consumer<MenuLayout> save = value -> {};
	private Runnable reset = () -> {};
	private boolean editable;
	private boolean refreshing;

	CapeMenuPanel(LayoutCodec codec)
	{
		this.codec = codec;
		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.add(new JLabel("Cape Menu Organizer"));
		content.add(text("Shared inventory and worn layout. Changes save automatically. Open your cape menu once to load destinations."));
		JScrollPane scroll = new JScrollPane(tree);
		scroll.setPreferredSize(new Dimension(200, 260));
		scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 360));
		content.add(scroll);
		tree.setRootVisible(true);
		tree.setShowsRootHandles(true);
		tree.setCellRenderer(new DefaultTreeCellRenderer()
		{
			@Override
			public Component getTreeCellRendererComponent(JTree owner, Object value, boolean selected,
				boolean expanded, boolean leaf, int row, boolean focus)
			{
				super.getTreeCellRendererComponent(owner, value, selected, expanded, leaf, row, focus);
				Object entry = ((DefaultMutableTreeNode) value).getUserObject();
				if (!selected && entry instanceof MenuLayout.Entry && ((MenuLayout.Entry) entry).color != null)
				{
					setForeground(new Color(((MenuLayout.Entry) entry).color));
				}
				return this;
			}
		});
		content.add(destinations);
		addButton("Add teleport", () ->
		{
			String destination = (String) destinations.getSelectedItem();
			if (destination != null)
			{
				String group = selectedGroup();
				edit(next -> next.addTeleport(group, destination));
			}
		});
		addButton("Add group", () ->
		{
			String name = JOptionPane.showInputDialog(this, "Group name:", "New group", JOptionPane.PLAIN_MESSAGE);
			if (name != null) { edit(next -> next.addGroup(name)); }
		});
		addButton("Rename", () ->
		{
			MenuLayout.Entry selected = selected();
			if (selected == null || !selected.isGroup()) { return; }
			String name = JOptionPane.showInputDialog(this, "Group name:", selected.name);
			if (name != null) { edit(next -> next.rename(selected.id, name)); }
		});
		addButton("Remove", () ->
		{
			MenuLayout.Entry selected = selected();
			if (selected == null) { return; }
			if (selected.isGroup() && !selected.children.isEmpty() && JOptionPane.showConfirmDialog(this,
				"Remove this group and its shortcuts?", "Remove group", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
			{
				return;
			}
			edit(next -> next.remove(selected.id));
		});
		addButton("Move up", () -> moveBy(-1));
		addButton("Move down", () -> moveBy(1));
		addButton("Move to…", this::moveTo);
		addButton("Color…", () ->
		{
			MenuLayout.Entry entry = selected();
			if (entry == null) { return; }
			Color color = JColorChooser.showDialog(this, "Entry color", entry.color == null ? Color.WHITE : new Color(entry.color));
			if (color != null) { edit(next -> next.setColor(entry.id, color.getRGB() & 0xffffff)); }
		});
		addButton("Reset color", () ->
		{
			MenuLayout.Entry entry = selected();
			if (entry != null) { edit(next -> next.setColor(entry.id, null)); }
		});
		JButton resetButton = new JButton("Reset layout");
		resetButton.setMargin(new Insets(4, 3, 4, 3));
		resetButton.addActionListener(event ->
		{
			if (JOptionPane.showConfirmDialog(this, "Replace the saved layout with defaults?", "Reset layout",
				JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
			{
				reset.run();
			}
		});
		controls.add(resetButton);
		content.add(controls);
		content.add(text("Select a group to add teleports inside it, or Main menu for direct shortcuts. A teleport can appear more than once."));
		visibility.setLayout(new BoxLayout(visibility, BoxLayout.Y_AXIS));
		content.add(new JLabel("Original teleport submenus"));
		content.add(visibility);
		content.add(text("Unassigned destinations stay under Other teleports. Your current left-click and Shift-click remain available."));
		content.add(status);
		for (Component child : content.getComponents())
		{
			if (child instanceof javax.swing.JComponent)
			{
				((javax.swing.JComponent) child).setAlignmentX(Component.LEFT_ALIGNMENT);
			}
		}
		add(content, BorderLayout.NORTH);
	}

	void showLayout(MenuLayout value, List<String> catalog, String message, boolean allowEdits,
		Consumer<MenuLayout> onSave, Runnable onReset)
	{
		layout = codec.copy(value);
		save = onSave;
		reset = onReset;
		editable = allowEdits;
		String selection = selected() == null ? null : selected().id;
		refreshTree(selection);
		Object oldDestination = destinations.getSelectedItem();
		destinations.removeAllItems();
		catalog.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).forEach(destinations::addItem);
		if (oldDestination != null && catalog.contains(oldDestination)) { destinations.setSelectedItem(oldDestination); }
		destinations.setEnabled(allowEdits);
		for (JButton button : editButtons) { button.setEnabled(allowEdits); }
		visibility.removeAll();
		refreshing = true;
		MenuLayout.ORIGINALS.stream().sorted().forEach(original ->
		{
			String[] parts = original.split(":", 2);
			String label = "inventory".equals(parts[0]) ? "Inventory: Teleports" : "Worn: " + parts[1];
			JCheckBox checkbox = new JCheckBox(label, !layout.hiddenOriginals.contains(original));
			checkbox.setToolTipText("Show the original " + parts[1] + " submenu in " + parts[0]);
			checkbox.setEnabled(allowEdits);
			checkbox.addActionListener(event ->
			{
				if (!refreshing)
				{
					edit(next ->
					{
						if (checkbox.isSelected()) { next.hiddenOriginals.remove(original); }
						else { next.hiddenOriginals.add(original); }
					});
				}
			});
			visibility.add(checkbox);
		});
		refreshing = false;
		status.setText(message);
		revalidate();
		repaint();
	}

	void setStatus(String message)
	{
		status.setText(message);
	}

	private static JTextArea text(String value)
	{
		JTextArea area = new JTextArea(value);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setEditable(false);
		area.setOpaque(false);
		area.setBorder(BorderFactory.createEmptyBorder(6, 0, 8, 0));
		area.setFocusable(false);
		area.setColumns(20);
		area.setRows(Math.max(3, (value.length() + 26) / 27));
		area.setMaximumSize(new Dimension(Integer.MAX_VALUE, area.getPreferredSize().height));
		return area;
	}

	private void addButton(String name, Runnable action)
	{
		JButton button = new JButton(name);
		button.setMargin(new Insets(4, 3, 4, 3));
		button.addActionListener(event -> action.run());
		controls.add(button);
		editButtons.add(button);
	}

	private void edit(Consumer<MenuLayout> action)
	{
		if (!editable) { return; }
		try
		{
			MenuLayout next = codec.copy(layout);
			action.accept(next);
			next.validate();
			String selection = selected() == null ? null : selected().id;
			layout = next;
			save.accept(codec.copy(next));
			refreshTree(selection);
		}
		catch (IllegalArgumentException ex)
		{
			status.setText(ex.getMessage());
		}
	}

	private void refreshTree(String selectedId)
	{
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Main menu");
		DefaultMutableTreeNode select = root;
		for (MenuLayout.Entry entry : layout.entries)
		{
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(entry);
			root.add(node);
			if (entry.id.equals(selectedId)) { select = node; }
			for (MenuLayout.Entry child : entry.children)
			{
				DefaultMutableTreeNode nested = new DefaultMutableTreeNode(child);
				node.add(nested);
				if (child.id.equals(selectedId)) { select = nested; }
			}
		}
		tree.setModel(new DefaultTreeModel(root));
		for (int row = 0; row < tree.getRowCount(); row++) { tree.expandRow(row); }
		tree.setSelectionPath(new TreePath(select.getPath()));
	}

	private MenuLayout.Entry selected()
	{
		Object node = tree.getLastSelectedPathComponent();
		if (!(node instanceof DefaultMutableTreeNode)) { return null; }
		Object value = ((DefaultMutableTreeNode) node).getUserObject();
		return value instanceof MenuLayout.Entry ? (MenuLayout.Entry) value : null;
	}

	private String selectedGroup()
	{
		MenuLayout.Entry entry = selected();
		if (entry == null) { return null; }
		if (entry.isGroup()) { return entry.id; }
		for (MenuLayout.Entry root : layout.entries)
		{
			if (root.children.contains(entry)) { return root.id; }
		}
		return null;
	}

	private void moveBy(int delta)
	{
		MenuLayout.Entry entry = selected();
		if (entry == null) { return; }
		List<MenuLayout.Entry> parent = layout.parent(entry.id);
		int index = parent.indexOf(entry) + delta;
		if (index >= 0 && index < parent.size())
		{
			String groupId = selectedGroup();
			if (entry.isGroup()) { groupId = null; }
			String target = groupId;
			edit(next -> next.move(entry.id, target, index));
		}
	}

	private void moveTo()
	{
		MenuLayout.Entry entry = selected();
		if (entry == null || entry.isGroup()) { return; }
		List<MenuLayout.Entry> groups = new ArrayList<>();
		groups.add(MenuLayout.Entry.group("Main menu"));
		for (MenuLayout.Entry root : layout.entries) { if (root.isGroup()) { groups.add(root); } }
		Object chosen = JOptionPane.showInputDialog(this, "Move shortcut to:", "Move shortcut",
			JOptionPane.PLAIN_MESSAGE, null, groups.toArray(), groups.get(0));
		if (chosen == null) { return; }
		String groupId = chosen == groups.get(0) ? null : ((MenuLayout.Entry) chosen).id;
		edit(next -> next.move(entry.id, groupId, Integer.MAX_VALUE));
	}
}

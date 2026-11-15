package com.capemenuorganizer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;

/** Swing state belongs to the EDT; saved changes are passed out as independent copies. */
final class CapeMenuPanel extends PluginPanel
{
	private final LayoutCodec codec;
	private final ColorPickerManager colorPickerManager;
	private RuneliteColorPicker activeColorPicker;
	private final JTree tree = new JTree();
	private final JComboBox<String> destinations = new JComboBox<>();
	private final JTextArea status = text("");
	private final JPanel controls = new JPanel(new GridLayout(0, 2, 4, 4));
	private final JPanel visibility = new JPanel();
	private final JPanel capeVisibility = new JPanel();
	private final JButton copyButton = new JButton();
	private Runnable copy = () -> {};
	private final List<JButton> editButtons = new ArrayList<>();
	private MenuLayout layout = new MenuLayout();
	private Consumer<MenuLayout> save = value -> {};
	private Runnable reset = () -> {};
	private boolean editable;
	private boolean refreshing;
	private long editorSession;

	CapeMenuPanel(LayoutCodec codec, ColorPickerManager colorPickerManager)
	{
		this.codec = codec;
		this.colorPickerManager = colorPickerManager;
		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		JLabel title = new JLabel("Cape Menu Organizer");
		title.setFont(FontManager.getRunescapeBoldFont());
		content.add(title);
		content.add(text("Applies to inventory and worn cape. Saves automatically."));
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
		addButton("Add action", () ->
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
			String name = promptForCurrentLayout(() -> JOptionPane.showInputDialog(this, "Group name:", "New group", JOptionPane.PLAIN_MESSAGE));
			if (name != null) { edit(next -> next.addGroup(name)); }
		});
		addButton("Rename", () ->
		{
			MenuLayout.Entry selected = selected();
			if (selected == null || !selected.isGroup()) { return; }
			String name = promptForCurrentLayout(() -> JOptionPane.showInputDialog(this, "Group name:", selected.name));
			if (name != null) { edit(next -> next.rename(selected.id, name)); }
		});
		addButton("Remove", () ->
		{
			MenuLayout.Entry selected = selected();
			if (selected == null) { return; }
			if (selected.isGroup() && !selected.children.isEmpty() && !Integer.valueOf(JOptionPane.OK_OPTION).equals(promptForCurrentLayout(() -> JOptionPane.showConfirmDialog(this,
				"Remove this group and its shortcuts?", "Remove group", JOptionPane.OK_CANCEL_OPTION))))
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
			RuneliteColorPicker picker = colorPickerManager.create(this,
				entry.color == null ? Color.WHITE : new Color(entry.color),
				getName() == null ? entry.toString() : getName() + " - " + entry, true);
			activeColorPicker = picker;
			// Capture the save session. A modeless picker must not write to a later profile/layout.
			MenuLayout beforeColor = codec.copy(layout);
			Consumer<MenuLayout> saveSession = save;
			picker.setOnClose(color ->
			{
				if (activeColorPicker == picker) { activeColorPicker = null; }
				beforeColor.setColor(entry.id, color.getRGB() & 0xffffff);
				saveSession.accept(codec.copy(beforeColor));
			});
			picker.setLocationRelativeTo(this);
			picker.setVisible(true);
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
			Runnable resetSession = reset;
			String prompt = editable ? "Replace this menu's layout with defaults?"
				: "Saved menus cannot be read. Replace both Normal and Shift layouts with defaults?";
			if (JOptionPane.showConfirmDialog(this, prompt, "Reset layout",
				JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
			{
				resetSession.run();
			}
		});
		controls.add(resetButton);
		content.add(controls);
		copyButton.setMargin(new Insets(4, 3, 4, 3));
		copyButton.addActionListener(event ->
		{
			Runnable copySession = copy;
			if (JOptionPane.showConfirmDialog(this, "Replace the other menu with a copy of this one?",
				"Copy menu", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) { copySession.run(); }
		});
		content.add(copyButton);
		content.add(text("Select Main menu or a group, then add actions."));
		visibility.setLayout(new BoxLayout(visibility, BoxLayout.Y_AXIS));
		JLabel originalsHeading = new JLabel("Show original menus");
		originalsHeading.setFont(FontManager.getRunescapeFont());
		originalsHeading.setForeground(ColorScheme.BRAND_ORANGE);
		originalsHeading.setToolTipText("Custom shortcuts remain available when original menus are hidden.");
		originalsHeading.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
		content.add(originalsHeading);
		JTextArea visibilityHelp = text("Checked = visible.");
		visibilityHelp.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
		visibilityHelp.setRows(1);
		visibilityHelp.setMaximumSize(new Dimension(Integer.MAX_VALUE, visibilityHelp.getPreferredSize().height));
		content.add(visibilityHelp);
		content.add(new JLabel("Teleports"));
		content.add(visibility);
		content.add(new JLabel("Cape actions"));
		capeVisibility.setLayout(new BoxLayout(capeVisibility, BoxLayout.Y_AXIS));
		content.add(capeVisibility);
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

	void setCopyAction(String target, boolean enabled, Runnable onCopy)
	{
		copyButton.setText("Copy to " + target);
		copyButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, copyButton.getPreferredSize().height));
		copyButton.setToolTipText("Replace " + target + " with this menu, including colors and visibility.");
		copyButton.setEnabled(enabled);
		copy = onCopy;
	}

	void showLayout(MenuLayout value, List<String> catalog, String message, boolean allowEdits,
		Consumer<MenuLayout> onSave, Runnable onReset)
	{
		showLayout(value, catalog, Collections.emptyMap(), message, allowEdits, onSave, onReset);
	}

	void showLayout(MenuLayout value, List<String> catalog, Map<String, String> capeActions,
		String message, boolean allowEdits, Consumer<MenuLayout> onSave, Runnable onReset)
	{
		editorSession++;
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
		capeVisibility.removeAll();
		Map<String, String> knownActions = new TreeMap<>(capeActions);
		knownActions.put("spellbook", "Spellbook");
		knownActions.put("features", "Features");
		for (Map.Entry<String, String> action : knownActions.entrySet())
		{
			String path = action.getKey();
			boolean parent = path.indexOf('/') < 0;
			String group = parent ? path : path.substring(0, path.indexOf('/'));
			String label = parent ? "Show " + action.getValue() : action.getValue().substring(action.getValue().indexOf(" / ") + 3);
			if ("features/teleport boat (pandemonium)".equals(path)) { label = "Teleport Boat"; }
			String display = label.length() > 24 ? "<html><body style='width:140px'>"
				+ label.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</body></html>" : label;
			JCheckBox checkbox = new JCheckBox(display, !layout.hiddenActions.contains(path));
			checkbox.setBorder(BorderFactory.createEmptyBorder(2, parent ? 0 : 14, 2, 0));
			checkbox.setToolTipText(action.getValue());
			checkbox.setEnabled(allowEdits && (parent || !layout.hiddenActions.contains(group)));
			checkbox.addActionListener(event ->
			{
				if (!refreshing)
				{
					edit(next ->
					{
						if (checkbox.isSelected()) { next.hiddenActions.remove(path); }
						else { next.hiddenActions.add(path); }
					});
				}
			});
			capeVisibility.add(checkbox);
		}
		refreshing = false;
		setStatus(message);
		revalidate();
		repaint();
	}

	void setStatus(String message)
	{
		status.setText(message);
		status.setRows(Math.max(1, ((message == null ? 0 : message.length()) + 26) / 27));
		status.setMaximumSize(new Dimension(Integer.MAX_VALUE, status.getPreferredSize().height));
		status.setVisible(message != null && !message.isEmpty());
	}

	<T> T promptForCurrentLayout(Supplier<T> prompt)
	{
		long session = editorSession;
		T result = prompt.get();
		if (session != editorSession)
		{
			setStatus("The menu changed while the dialog was open. Try again.");
			return null;
		}
		return result;
	}

	void closeColorPicker()
	{
		RuneliteColorPicker picker = activeColorPicker;
		activeColorPicker = null;
		if (picker != null)
		{
			picker.setOnClose(null);
			picker.dispatchEvent(new WindowEvent(picker, WindowEvent.WINDOW_CLOSING));
		}
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
		area.setRows(Math.max(1, (value.length() + 26) / 27));
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
			setStatus(ex.getMessage());
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
		Object chosen = promptForCurrentLayout(() -> JOptionPane.showInputDialog(this, "Move shortcut to:", "Move shortcut",
			JOptionPane.PLAIN_MESSAGE, null, groups.toArray(), groups.get(0)));
		if (chosen == null) { return; }
		String groupId = chosen == groups.get(0) ? null : ((MenuLayout.Entry) chosen).id;
		edit(next -> next.move(entry.id, groupId, Integer.MAX_VALUE));
	}
}

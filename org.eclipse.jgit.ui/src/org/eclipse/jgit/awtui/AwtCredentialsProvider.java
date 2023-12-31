/*
 * Copyright (C) 2010, Google Inc.
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.awtui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.ChainingCredentialsProvider;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.NetRCCredentialsProvider;
import org.eclipse.jgit.transport.URIish;

/**
 * Interacts with the user during authentication by using AWT/Swing dialogs.
 */
public class AwtCredentialsProvider extends CredentialsProvider {
	/**
	 * Install this implementation as the default.
	 */
	public static void install() {
		final AwtCredentialsProvider c = new AwtCredentialsProvider();
		CredentialsProvider cp = new ChainingCredentialsProvider(
				new NetRCCredentialsProvider(), c);
		CredentialsProvider.setDefault(cp);
	}

	@Override
	public boolean isInteractive() {
		return true;
	}

	@Override
	public boolean supports(CredentialItem... items) {
		for (CredentialItem i : items) {
			if (i instanceof CredentialItem.StringType)
				continue;

			else if (i instanceof CredentialItem.CharArrayType)
				continue;

			else if (i instanceof CredentialItem.YesNoType)
				continue;

			else if (i instanceof CredentialItem.InformationalMessage)
				continue;

			else
				return false;
		}
		return true;
	}

	@Override
	public boolean get(URIish uri, CredentialItem... items)
			throws UnsupportedCredentialItem {
		switch (items.length) {
		case 0:
			return true;
		case 1:
			final CredentialItem item = items[0];

			if (item instanceof CredentialItem.InformationalMessage) {
				JOptionPane.showMessageDialog(null, item.getPromptText(),
						UIText.get().warning, JOptionPane.INFORMATION_MESSAGE);
				return true;

			} else if (item instanceof CredentialItem.YesNoType) {
				CredentialItem.YesNoType v = (CredentialItem.YesNoType) item;
				int r = JOptionPane.showConfirmDialog(null, v.getPromptText(),
						UIText.get().warning, JOptionPane.YES_NO_OPTION);
				switch (r) {
				case JOptionPane.YES_OPTION:
					v.setValue(true);
					return true;

				case JOptionPane.NO_OPTION:
					v.setValue(false);
					return true;

				case JOptionPane.CANCEL_OPTION:
				case JOptionPane.CLOSED_OPTION:
				default:
					return false;
				}

			} else {
				return interactive(uri, items);
			}
		default:
			return interactive(uri, items);
		}
	}

	private static boolean interactive(URIish uri, CredentialItem[] items) {
		final GridBagConstraints gbc = new GridBagConstraints(0, 0, 1, 1, 1, 1,
				GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 0);
		final JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());

		final JTextField[] texts = new JTextField[items.length];
		for (int i = 0; i < items.length; i++) {
			CredentialItem item = items[i];

			if (item instanceof CredentialItem.StringType
					|| item instanceof CredentialItem.CharArrayType) {
				gbc.fill = GridBagConstraints.NONE;
				gbc.gridwidth = GridBagConstraints.RELATIVE;
				gbc.gridx = 0;
				panel.add(new JLabel(item.getPromptText()), gbc);

				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.gridwidth = GridBagConstraints.RELATIVE;
				gbc.gridx = 1;
				if (item.isValueSecure())
					texts[i] = new JPasswordField(20);
				else
					texts[i] = new JTextField(20);
				panel.add(texts[i], gbc);
				gbc.gridy++;

			} else if (item instanceof CredentialItem.InformationalMessage) {
				gbc.fill = GridBagConstraints.NONE;
				gbc.gridwidth = GridBagConstraints.REMAINDER;
				gbc.gridx = 0;
				panel.add(new JLabel(item.getPromptText()), gbc);
				gbc.gridy++;

			} else {
				throw new UnsupportedCredentialItem(uri, item.getPromptText());
			}
		}

		if (JOptionPane.showConfirmDialog(null, panel,
				UIText.get().authenticationRequired,
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION)
			return false; // cancel

		for (int i = 0; i < items.length; i++) {
			CredentialItem item = items[i];
			JTextField f = texts[i];

			if (item instanceof CredentialItem.StringType) {
				CredentialItem.StringType v = (CredentialItem.StringType) item;
				if (f instanceof JPasswordField)
					v.setValue(new String(((JPasswordField) f).getPassword()));
				else
					v.setValue(f.getText());

			} else if (item instanceof CredentialItem.CharArrayType) {
				CredentialItem.CharArrayType v = (CredentialItem.CharArrayType) item;
				if (f instanceof JPasswordField)
					v.setValueNoCopy(((JPasswordField) f).getPassword());
				else
					v.setValueNoCopy(f.getText().toCharArray());
			}
		}
		return true;
	}
}

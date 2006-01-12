/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry;

import org.eclipse.core.runtime.Assert;

// This object is used to keep track on a contributor basis of the extension and extension points being contributed.
// It is mainly used on removal so we can quickly  find objects to remove.
// Each contribution is made in the context of a namespace.  
public class Contribution implements KeyedElement {
	static final int[] EMPTY_CHILDREN = new int[] {0, 0};

	//The registry that owns this object
	protected ExtensionRegistry registry;

	// The actual contributor of the contribution.
	final protected String contributorId;

	// cached Id of the namespace owner (might be same or different from the contributorId)
	// null if it is not cached yet or no namespace was found during previous cache attempt. 
	private String namespaceOwnerId = null;

	// indicates if this contribution needs to be saved in the registry cache
	protected boolean isDynamic;

	// This array stores the identifiers of both the extension points and the extensions.
	// The array has always a minimum size of 2.
	// The first element of the array is the number of extension points and the second the number of extensions. 
	// [numberOfExtensionPoints, numberOfExtensions, extensionPoint#1, extensionPoint#2, extensionPoint..., ext#1, ext#2, ext#3, ... ].
	// The size of the array is 2 + (numberOfExtensionPoints +  numberOfExtensions).
	private int[] children = EMPTY_CHILDREN;
	static final public byte EXTENSION_POINT = 0;
	static final public byte EXTENSION = 1;

	protected Contribution(String contributorId, ExtensionRegistry registry, boolean dynamic) {
		this.contributorId = contributorId;
		this.registry = registry;
		this.isDynamic = dynamic;
	}

	void mergeContribution(Contribution addContribution) {
		Assert.isTrue(contributorId.equals(addContribution.contributorId));
		Assert.isTrue(registry == addContribution.registry);

		// isDynamic?
		// Old New Result
		// F   F   F
		// F   T   F
		// T   F   F	=> the only situation where isDynamic status needs to be adjusted 
		// T   T   T
		if (isDynamic() && !addContribution.isDynamic())
			isDynamic = false;

		int[] existing = getRawChildren();
		int[] addition = addContribution.getRawChildren();

		int extensionPoints = existing[EXTENSION_POINT] + addition[EXTENSION_POINT];
		int extensions = existing[EXTENSION] + addition[EXTENSION];
		int[] allChildren = new int[2 + extensionPoints + extensions];

		allChildren[EXTENSION_POINT] = extensionPoints;
		System.arraycopy(existing, 2, allChildren, 2, existing[EXTENSION_POINT]);
		System.arraycopy(addition, 2, allChildren, 2 + existing[EXTENSION_POINT], addition[EXTENSION_POINT]);
		allChildren[EXTENSION] = extensions;
		System.arraycopy(existing, 2 + existing[EXTENSION_POINT], allChildren, 2 + extensionPoints, existing[EXTENSION]);
		System.arraycopy(addition, 2 + addition[EXTENSION_POINT], allChildren, 2 + extensionPoints + existing[EXTENSION], addition[EXTENSION]);

		children = allChildren;
	}

	void setRawChildren(int[] children) {
		this.children = children;
	}

	protected String getContributorId() {
		return contributorId;
	}

	protected int[] getRawChildren() {
		return children;
	}

	protected int[] getExtensions() {
		int[] results = new int[children[EXTENSION]];
		System.arraycopy(children, 2 + children[EXTENSION_POINT], results, 0, children[EXTENSION]);
		return results;
	}

	protected int[] getExtensionPoints() {
		int[] results = new int[children[EXTENSION_POINT]];
		System.arraycopy(children, 2, results, 0, children[EXTENSION_POINT]);
		return results;
	}

	public String getNamespace() {
		return registry.getNamespace(contributorId);
	}

	public String toString() {
		return "Contribution: " + contributorId + " in namespace" + getNamespace(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected String getNamespaceOwnerId() {
		// Performance: this function is not called during warm Eclipse startup using cached 
		// extension registry, but is called about 45 times per contribution during 
		// the "clean" Eclipse start. Cache the result.
		if (namespaceOwnerId == null)
			namespaceOwnerId = registry.getNamespaceOwnerId(contributorId);
		return namespaceOwnerId;
	}

	//Implements the KeyedElement interface
	public int getKeyHashCode() {
		return getKey().hashCode();
	}

	public Object getKey() {
		return contributorId;
	}

	public boolean compare(KeyedElement other) {
		return contributorId.equals(((Contribution) other).contributorId);
	}

	public boolean isDynamic() {
		return isDynamic;
	}
}

/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.registry.editor;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EntityEditorsRegistry
 */
public class EntityEditorsRegistry {

    private static final String TAG_EDITOR = "editor"; //NON-NLS-1
    private static final String TAG_MANAGER = "manager"; //NON-NLS-1

    private EntityEditorDescriptor defaultEditor;
    private List<EntityEditorDescriptor> entityEditors = new ArrayList<EntityEditorDescriptor>();
    private Map<String, List<EntityEditorDescriptor>> positionsMap = new HashMap<String, List<EntityEditorDescriptor>>();
    private List<EntityManagerDescriptor> entityManagers = new ArrayList<EntityManagerDescriptor>();

    public EntityEditorsRegistry(IExtensionRegistry registry)
    {
        // Create default editor
        defaultEditor = new EntityEditorDescriptor();
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EntityEditorDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if (TAG_EDITOR.equals(ext.getName())) {
                EntityEditorDescriptor descriptor = new EntityEditorDescriptor(ext);
                entityEditors.add(descriptor);
                List<EntityEditorDescriptor> list = positionsMap.get(descriptor.getPosition());
                if (list == null) {
                    list = new ArrayList<EntityEditorDescriptor>();
                    positionsMap.put(descriptor.getPosition(), list);
                }
                list.add(descriptor);
            } else if (TAG_MANAGER.equals(ext.getName())) {
                EntityManagerDescriptor descriptor = new EntityManagerDescriptor(ext);
                entityManagers.add(descriptor);
            }
        }

    }

    public void dispose()
    {
        entityEditors.clear();

        for (EntityManagerDescriptor descriptor : entityManagers) {
            descriptor.dispose();
        }
        entityManagers.clear();
    }

    public EntityEditorDescriptor getMainEntityEditor(DBPObject object)
    {
        for (EntityEditorDescriptor descriptor : entityEditors) {
            if (descriptor.appliesTo(object) && descriptor.isMain() && descriptor.getType() == EntityEditorDescriptor.Type.editor) {
                return descriptor;
            }
        }
        return defaultEditor;
    }

    public List<EntityEditorDescriptor> getEntityEditors(DBPObject object, String position)
    {
        List<EntityEditorDescriptor> editors = new ArrayList<EntityEditorDescriptor>();
        final List<EntityEditorDescriptor> positionList =
            CommonUtils.isEmpty(position) ? entityEditors : positionsMap.get(position);
        if (positionList != null) {
            for (EntityEditorDescriptor descriptor : positionList) {
                if (descriptor.appliesTo(object)) {
                    editors.add(descriptor);
                }
            }
        }
        return editors;
    }

    private EntityManagerDescriptor getEntityManager(Class objectType)
    {
        for (EntityManagerDescriptor descriptor : entityManagers) {
            if (descriptor.appliesToType(objectType)) {
                return descriptor;
            }
        }
        return null;
    }

    public DBEObjectManager<?> getObjectManager(Class<?> aClass)
    {
        EntityManagerDescriptor entityManager = getEntityManager(aClass);
        return entityManager == null ? null : entityManager.getManager();
    }

    public <T> T getObjectManager(Class<?> objectClass, Class<T> managerType)
    {
        final DBEObjectManager<?> objectManager = getObjectManager(objectClass);
        if (objectManager != null && managerType.isAssignableFrom(objectManager.getClass())) {
            return managerType.cast(objectManager);
        } else {
            return null;
        }
    }

}

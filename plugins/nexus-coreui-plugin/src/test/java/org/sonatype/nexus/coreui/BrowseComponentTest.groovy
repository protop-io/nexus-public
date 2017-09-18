/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.coreui

import org.sonatype.nexus.common.entity.EntityId
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.storage.BrowseNode
import org.sonatype.nexus.repository.storage.BrowseNodeStore
import spock.lang.Specification
import spock.lang.Subject

/**
 * @since 3.1
 */
class BrowseComponentTest
    extends Specification
{
  private static final String REPOSITORY_NAME = 'repositoryName'
  private static final String ROOT = '/'

  BrowseNodeStore browseNodeStore = Mock()

  EntityId assetId = Mock()

  EntityId componentId = Mock()

  RepositoryManager repositoryManager = Mock()

  Repository repository = Mock()

  @Subject
  BrowseComponent browseComponent = new BrowseComponent(browseNodeStore: browseNodeStore, repositoryManager: repositoryManager)

  def "Root node list query"() {
    given: 'These test objects'
      def browseNodes = [new BrowseNode(path: 'com'),
                         new BrowseNode(path: 'org', componentId: componentId),
                         new BrowseNode(path: 'net', assetId: assetId)]

    when: 'Requesting the list of root nodes'
      TreeStoreLoadParameters treeStoreLoadParameters = new TreeStoreLoadParameters(
          repositoryName: REPOSITORY_NAME,
          node: ROOT,
          filter: 'foo')

      1 * repositoryManager.get(REPOSITORY_NAME) >> repository
      1 * browseNodeStore.getChildrenByPath(repository, [], 'foo') >> browseNodes
      List<BrowseNodeXO> xos = browseComponent.read(treeStoreLoadParameters)

    then: 'the 3 root entries are returned'
      xos*.type == ['folder', 'component', 'asset']
      xos*.text == ['com', 'org', 'net']
      xos*.id   == ['com', 'org', 'net']
      xos*.leaf == [false, false, true]
  }

  def "non-root list query"() {
    given: 'These test objects'
      def browseNodes = [new BrowseNode(path: 'com'),
                         new BrowseNode(path: 'org', componentId: componentId),
                         new BrowseNode(path: 'net', assetId: assetId)]

    when: 'Requesting the list of root nodes'
      TreeStoreLoadParameters treeStoreLoadParameters = new TreeStoreLoadParameters(
          repositoryName: REPOSITORY_NAME,
          node: 'com/boogie/down',
          filter: null)

      1 * repositoryManager.get(REPOSITORY_NAME) >> repository
      1 * browseNodeStore.getChildrenByPath(repository, ['com','boogie','down'], null) >> browseNodes
      List<BrowseNodeXO> xos = browseComponent.read(treeStoreLoadParameters)

    then: 'the 3 entries are returned'
      xos*.type == ['folder', 'component', 'asset']
      xos*.text == ['com', 'org', 'net']
      xos*.id   == ['com/boogie/down/com', 'com/boogie/down/org', 'com/boogie/down/net']
      xos*.leaf == [false, false, true]
  }

  def 'validate encoded segments'() {
    given: 'These test objects'
    def browseNodes = [new BrowseNode(path: 'com'),
                       new BrowseNode(path: 'org', componentId: componentId),
                       new BrowseNode(path: 'n/e/t', assetId: assetId)]

    when: 'Requesting the list of root nodes'
    TreeStoreLoadParameters treeStoreLoadParameters = new TreeStoreLoadParameters(
        repositoryName: REPOSITORY_NAME,
        node: 'com/boo%2Fgie/down')

      1 * repositoryManager.get(REPOSITORY_NAME) >> repository
      1 * browseNodeStore.getChildrenByPath(repository, ['com','boo/gie','down'], null) >> browseNodes
    List<BrowseNodeXO> xos = browseComponent.read(treeStoreLoadParameters)

    then: 'the 3 entries are returned'
      xos*.type == ['folder', 'component', 'asset']
      xos*.text == ['com', 'org', 'n/e/t']
      xos*.id   == ['com/boo%2Fgie/down/com', 'com/boo%2Fgie/down/org', 'com/boo%2Fgie/down/n%2Fe%2Ft']
      xos*.leaf == [false, false, true]
  }

  def 'browse nodes are sorted'() {
    given: 'This repository and a list of browse nodes'
      repositoryManager.get(REPOSITORY_NAME) >> repository
      browseNodeStore.getChildrenByPath(repository, [], null) >> [
          new BrowseNode(path: '1.0', assetId: assetId),
          new BrowseNode(path: '1.10-alpha', assetId: assetId),
          new BrowseNode(path: '1.10', assetId: assetId),
          new BrowseNode(path: '1.2', assetId: assetId),
          new BrowseNode(path: 'z'),
          new BrowseNode(path: 'a'),
          new BrowseNode(path: '2')
      ]


    when: 'Requesting the list of root nodes'
      TreeStoreLoadParameters parameters = new TreeStoreLoadParameters(repositoryName: REPOSITORY_NAME, node: ROOT)
      List<BrowseNodeXO> xos = browseComponent.read(parameters)

    then: 'the entries are sorted properly'
      xos*.text == ['a', 'z', '2', '1.0', '1.2', '1.10-alpha', '1.10']
  }
}

/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.db;

import java.io.InputStream;
import java.util.Date;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.Tag;

import com.axelor.common.ClassUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;

/**
 * This class can be used to load test data for JPA entities.
 * 
 * <p>
 * It processes YAML files located in {@code /fixtures}.
 * 
 * <p>
 * For example, the following schema:
 * 
 * <pre>
 * 
 * &#64;Entity
 * &#64;Table(name = "CONTACT_CIRCLE")
 * public class Circle extends Model {
 *     private String code;
 *     private String name;
 *     ...
 *     ...
 * }
 * 
 * &#64;Entity
 * &#64;Table(name = "CONTACT_CONTACT")
 * public class Contact extends Model {
 *     private String firstName;
 *     private String lastName;
 *     private String email;
 *     
 *     &#64;ManyToMany
 *     private Set<Circle> circles;
 *     ...
 *     ...
 *     ...
 * </pre>
 * 
 * The fixtures should be defined like this:
 * 
 * <pre>
 *  - !Circle: &family
 *   code: family
 *   name: Family
 * 
 * - !Circle: &friends
 *   code: friends
 *   name: Friends
 *   
 * - !Circle: &business
 *   code: business
 *   name: Business
 * 
 * - !Contact:
 *   firstName: John
 *   lastName: Smith
 *   email: j.smith@gmail.com
 *   circles:
 *     - *friends
 *     - *business
 *   
 * - !Contact:
 *   firstName: Tin
 *   lastName: Tin
 *   email: tin.tin@gmail.com
 *   circles:
 *     - *business
 * </pre>
 * 
 * <p>
 * In order to use the fixture data, the {@link JpaFixture} must be injected.
 * 
 * <pre>
 * &#64;RunWith(GuiceRunner.class)
 * &#64;GuiceModules({MyModule.class})
 * class FixtureTest {
 * 
 *     &#64;Inject
 *     private JpaFixture fixture;
 *     
 *     &#64;Before
 *     public void setUp() {
 *         fixture.load("demo-data.yml");
 *         fixture.load("demo-data-extra.yml");
 *     }
 *     
 *     &#64;Test
 *     public void testCount() {
 *         Assert.assertEqual(2, Contact.all().count());
 *         ...
 *     }
 *     ...
 * }
 * </pre>
 */
public class JpaFixture {

	private InputStream read(String resource) {
		return ClassUtils.getResourceStream("fixtures/" + resource);
	}

	@Transactional
	public void load(String fixture) {

		final InputStream stream = read(fixture);
		final Map<Node, Object> objects = Maps.newLinkedHashMap();

		if (stream == null) {
			throw new IllegalArgumentException("No such fixture found: " + fixture);
		}
		
		final Constructor ctor = new Constructor() {
			{
				yamlClassConstructors.put(NodeId.scalar, new TimeStampConstruct());
			}
			
			class TimeStampConstruct extends Constructor.ConstructScalar {

				Construct dateConstructor = yamlConstructors.get(Tag.TIMESTAMP);
				
				@Override
				public Object construct(Node nnode) {
					if (nnode.getTag().equals(Tag.TIMESTAMP)) {
						Date date = (Date) dateConstructor.construct(nnode);
						if (nnode.getType() == LocalDate.class) {
							return new LocalDate(date, DateTimeZone.UTC);
						}
						if (nnode.getType() == LocalDateTime.class) {
							return new LocalDateTime(date, DateTimeZone.UTC);
						}
						return new DateTime(date, DateTimeZone.UTC);
					} else {
						return super.construct(nnode);
					}
				}

			}
			
			@Override
			protected Object constructObject(Node node) {
				
				Object obj = super.constructObject(node);
				
				if (objects.containsKey(node)) {
					return objects.get(node);
				}
				
				if (obj instanceof Model) {
					objects.put(node, obj);
					return obj;
				}
				return obj;
			}
		};
		
		for(Class<?> klass : JPA.models()) {
			ctor.addTypeDescription(new TypeDescription(klass, "!" + klass.getSimpleName() + ":"));
		}

		Yaml data = new Yaml(ctor);
		data.load(stream);
		
		for(Object item : Lists.reverse(Lists.newArrayList(objects.values()))) {
			try {
				JPA.manage((Model) item);
			}catch(Exception e) {
			}
		}
	}
}
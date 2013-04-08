/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.faq.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SARL
 * May 4, 2009, 5:24:16 PM
 */
public class CategoryTree {
  private Category category;

  private List<CategoryTree> subCategory;
  
  public CategoryTree() {
    category = new Category();
    subCategory = new ArrayList<CategoryTree>();
  }

  public Category getCategory() {
    return category;
  }

  public void setCategory(Category category) {
    this.category = category;
  }

  public List<CategoryTree> getSubCategory() {
    return subCategory;
  }

  public void setSubCategory(List<CategoryTree> subCategory) {
    this.subCategory = subCategory;
  } 
}

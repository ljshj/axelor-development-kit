package com.axelor.meta.loader;

import java.util.List;
import java.util.regex.Pattern;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

final class Module {

	private String name;
	
	private List<Module> depends = Lists.newArrayList();
	
	private String version;
	
	private String installedVersion;
	
	private boolean installed = false;
	
	private boolean removable = false;

	public Module(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	public List<Module> getDepends() {
		return depends;
	}
	
	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
	
	public String getInstalledVersion() {
		return installedVersion;
	}
	
	public void setInstalledVersion(String installedVersion) {
		this.installedVersion = installedVersion;
	}
	
	public boolean isInstalled() {
		return installed;
	}
	
	public void setInstalled(boolean installed) {
		this.installed = installed;
	}
	
	public boolean isRemovable() {
		return removable;
	}
	
	public void setRemovable(boolean removable) {
		this.removable = removable;
	}
	
	public boolean isUpgradable() {
		return installed && !Objects.equal(version, installedVersion);
	}
	
	public void dependsOn(Module module) {
		if (!depends.contains(module)) {
			depends.add(module);
		}
	}
	
	private Pattern pat;
	
	public boolean hasEntity(Class<?> klass) {
		if (pat == null) {
			String mod = name.replace("axelor-", "");
			if ("core".equals(mod)) {
				mod = "(core|auth|meta)";
			}
			pat = Pattern.compile("\\." + mod + "\\.db\\.");
		}
		return pat.matcher(klass.getName()).find();
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(Module.class.getName(), name);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null) return false;
		if (!(obj instanceof Module)) return false;
		return name.equals(((Module) obj).name);
	}
	
	public String pprint(int depth) {
		StringBuilder builder = new StringBuilder();
		builder.append(name).append("\n");
		for(Module dep : depends) {
			builder.append(Strings.repeat("  ", depth))
				   .append("-> ")
				   .append(dep.pprint(depth+1));
		}
		return builder.toString();
	}
	
	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("name", name)
				.add("version", version)
				.toString();
	}
}